#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include <dirent.h>
#include <sys/stat.h>
#include <ctime>
#include <sstream>
#include <iomanip>
#include <locale>
#include <codecvt>
#include <cstring>
#include <limits.h>

struct FileInfo {
    std::string name;
    std::string details;
    bool isDirectory;
    long long size;
    time_t lastModified;
};

std::string formatSize(long long bytes) {
    if (bytes <= 0) return "0 B";

    const char *units[] = {"B", "kB", "MB", "GB", "TB"};
    int unitIndex = 0;
    double size = static_cast<double>(bytes);

    while (size >= 1024 && unitIndex < 4) {
        size /= 1024;
        unitIndex++;
    }

    std::ostringstream oss;
    if (unitIndex == 0) {
        oss << static_cast<long long>(size) << " " << units[unitIndex];
    } else {
        oss << std::fixed << std::setprecision(size >= 100 ? 0 : (size >= 10 ? 1 : 2))
            << size << " " << units[unitIndex];
    }
    return oss.str();
}

std::string formatTime(time_t time, const std::string &format = "MMM dd, hh:mm a") {
    struct tm *timeinfo = localtime(&time);
    char buffer[100];

    if (format == "MMM dd, hh:mm a") {
        strftime(buffer, sizeof(buffer), "%b %d, %I:%M %p", timeinfo);
    } else {
        strftime(buffer, sizeof(buffer), format.c_str(), timeinfo);
    }

    return std::string(buffer);
}

std::string getFolderDetails(const std::string &path) {
    DIR *dir = opendir(path.c_str());
    if (!dir) return "Insufficient Permissions";

    int filesCount = 0;
    int foldersCount = 0;
    struct dirent *entry;

    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        std::string fullPath = path + "/" + entry->d_name;
        struct stat statbuf;
        if (stat(fullPath.c_str(), &statbuf) == 0) {
            if (S_ISDIR(statbuf.st_mode)) {
                foldersCount++;
            } else {
                filesCount++;
            }
        }
    }
    closedir(dir);

    std::string foldersStr =
            std::to_string(foldersCount) + (foldersCount == 1 ? " folder" : " folders");
    std::string filesStr = std::to_string(filesCount) + (filesCount == 1 ? " file" : " files");

    return foldersStr + ", " + filesStr;
}

std::vector<FileInfo> getFileList(const std::string &path, int sortOrder) {
    std::vector<FileInfo> files;
    DIR *dir = opendir(path.c_str());
    if (!dir) return files;

    struct dirent *entry;
    while ((entry = readdir(dir)) != nullptr) {
        if (strcmp(entry->d_name, ".") == 0 || strcmp(entry->d_name, "..") == 0) {
            continue;
        }

        std::string fullPath = path + "/" + entry->d_name;
        struct stat statbuf;
        if (stat(fullPath.c_str(), &statbuf) != 0) continue;

        FileInfo fileInfo;
        fileInfo.name = entry->d_name;
        fileInfo.isDirectory = S_ISDIR(statbuf.st_mode);
        fileInfo.size = statbuf.st_size;
        fileInfo.lastModified = statbuf.st_mtime;

        if (fileInfo.isDirectory) {
            fileInfo.details = getFolderDetails(fullPath);
        } else {
            fileInfo.details =
                    formatTime(fileInfo.lastModified) + " | " + formatSize(fileInfo.size);
        }

        files.push_back(fileInfo);
    }
    closedir(dir);

    // Sort files
    std::sort(files.begin(), files.end(), [sortOrder](const FileInfo &a, const FileInfo &b) {
        // Directories first
        if (a.isDirectory != b.isDirectory) {
            return a.isDirectory > b.isDirectory;
        }

        switch (sortOrder) {
            case 0: // NAME_ASCENDING
                return strcasecmp(a.name.c_str(), b.name.c_str()) < 0;
            case 1: // NAME_DESCENDING
                return strcasecmp(a.name.c_str(), b.name.c_str()) > 0;
            case 2: // DATE_ASCENDING
                return a.lastModified < b.lastModified;
            case 3: // DATE_DESCENDING
                return a.lastModified > b.lastModified;
            case 4: // SIZE_ASCENDING
                return a.size < b.size;
            case 5: // SIZE_DESCENDING
                return a.size > b.size;
            default:
                return strcasecmp(a.name.c_str(), b.name.c_str()) < 0;
        }
    });

    return files;
}

jstring safeNewStringUTF(JNIEnv *env, const char *utf8) {
    if (!utf8) return nullptr;
    // Check for valid UTF-8
    const unsigned char *bytes = (const unsigned char *) utf8;
    while (*bytes) {
        if (*bytes < 0x80) {
            bytes++;
        } else if ((*bytes & 0xE0) == 0xC0) {
            if ((bytes[1] & 0xC0) != 0x80) goto fallback;
            bytes += 2;
        } else if ((*bytes & 0xF0) == 0xE0) {
            if ((bytes[1] & 0xC0) != 0x80 || (bytes[2] & 0xC0) != 0x80) goto fallback;
            bytes += 3;
        } else if ((*bytes & 0xF8) == 0xF0) {
            if ((bytes[1] & 0xC0) != 0x80 || (bytes[2] & 0xC0) != 0x80 ||
                (bytes[3] & 0xC0) != 0x80)
                goto fallback;
            bytes += 4;
        } else {
            goto fallback;
        }
    }
    return env->NewStringUTF(utf8);
    fallback:
    // Fallback: treat as ISO-8859-1 and convert to UTF-16
    size_t len = strlen(utf8);
    jchar *utf16 = new jchar[len];
    for (size_t i = 0; i < len; ++i) utf16[i] = (unsigned char) utf8[i];
    jstring result = env->NewString(utf16, len);
    delete[] utf16;
    return result;
}

extern "C" {

JNIEXPORT jobjectArray JNICALL
Java_dev_pranav_macaw_util_FileJNI_getOrderedChildren(JNIEnv *env, jobject, jstring jpath,
                                                      jint sortOrder) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    char canonicalDir[PATH_MAX];
    std::string basePath;
    if (realpath(path, canonicalDir)) {
        basePath = canonicalDir;
    } else {
        basePath = path;
    }
    std::vector<FileInfo> files = getFileList(basePath, sortOrder);
    env->ReleaseStringUTFChars(jpath, path);

    // Find FileEntry class
    jclass fileEntryClass = env->FindClass("dev/pranav/macaw/util/FileEntry");
    if (!fileEntryClass) return nullptr;

    // Get constructor
    jmethodID constructor = env->GetMethodID(fileEntryClass, "<init>",
                                             "(Ljava/lang/String;Ljava/lang/String;ZJJLjava/lang/String;)V");
    if (!constructor) return nullptr;

    // Create array
    jobjectArray result = env->NewObjectArray(files.size(), fileEntryClass, nullptr);
    if (!result) return nullptr;

    for (size_t i = 0; i < files.size(); i++) {
        std::string absPath = basePath + "/" + files[i].name;
        jstring jname = safeNewStringUTF(env, files[i].name.c_str());
        jstring jdetails = safeNewStringUTF(env, files[i].details.c_str());
        jboolean jisDirectory = files[i].isDirectory;
        jlong jlastModified = static_cast<jlong>(files[i].lastModified);
        jlong jsize = static_cast<jlong>(files[i].size);
        jstring jabsPath = safeNewStringUTF(env, absPath.c_str());
        jobject fileEntry = env->NewObject(fileEntryClass, constructor, jname, jdetails,
                                           jisDirectory, jlastModified, jsize, jabsPath);
        env->SetObjectArrayElement(result, i, fileEntry);
        env->DeleteLocalRef(jname);
        env->DeleteLocalRef(jdetails);
        env->DeleteLocalRef(jabsPath);
        env->DeleteLocalRef(fileEntry);
    }

    return result;
}

JNIEXPORT jstring JNICALL
Java_dev_pranav_macaw_util_FileJNI_getFileDetails(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    struct stat statbuf;

    std::string details;
    if (stat(path, &statbuf) == 0) {
        if (S_ISDIR(statbuf.st_mode)) {
            details = getFolderDetails(std::string(path));
        } else {
            details = formatTime(statbuf.st_mtime) + " | " + formatSize(statbuf.st_size);
        }
    } else {
        details = "Error accessing file";
    }

    env->ReleaseStringUTFChars(jpath, path);
    return env->NewStringUTF(details.c_str());
}

JNIEXPORT jstring JNICALL
Java_dev_pranav_macaw_util_FileJNI_getLastModifiedFormatted(JNIEnv *env, jobject, jstring jpath,
                                                            jstring jformat) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    const char *format = env->GetStringUTFChars(jformat, nullptr);

    struct stat statbuf;
    std::string result;

    if (stat(path, &statbuf) == 0) {
        result = formatTime(statbuf.st_mtime, std::string(format));
    } else {
        result = "";
    }

    env->ReleaseStringUTFChars(jpath, path);
    env->ReleaseStringUTFChars(jformat, format);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_dev_pranav_macaw_util_FileJNI_getSizeString(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    struct stat statbuf;

    std::string result;
    if (stat(path, &statbuf) == 0) {
        result = formatSize(statbuf.st_size);
    } else {
        result = "0 B";
    }

    env->ReleaseStringUTFChars(jpath, path);
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT jstring JNICALL
Java_dev_pranav_macaw_util_FileJNI_getFolderContentsCount(JNIEnv *env, jobject, jstring jpath) {
    const char *path = env->GetStringUTFChars(jpath, nullptr);
    std::string result = getFolderDetails(std::string(path));
    env->ReleaseStringUTFChars(jpath, path);
    return env->NewStringUTF(result.c_str());
}

}
