cmake_minimum_required(VERSION 3.7)
project(binscure_lib)

set(CMAKE_CXX_STANDARD 11)

find_package(JNI)

if (JNI_FOUND)
    message (STATUS "JNI_INCLUDE_DIRS=${JNI_INCLUDE_DIRS}")
    message (STATUS "JNI_LIBRARIES=${JNI_LIBRARIES}")
    include_directories(${JNI_INCLUDE_DIRS})
endif()

set(SOURCE_FILES BinscureNativeRuntime.h)
add_library(binscure_lib SHARED ${SOURCE_FILES})
