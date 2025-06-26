cmake_minimum_required(VERSION 3.22.1)

project("fileops")

add_library(${CMAKE_PROJECT_NAME} SHARED
        fileops.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log)
