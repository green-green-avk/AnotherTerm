cmake_minimum_required(VERSION 3.22.1)
project(PtyProcess)
add_compile_definitions(PKG_NAME="green_green_avk/ptyprocess")
add_compile_options(-std=c++17 -fno-rtti -fno-exceptions -fvisibility=hidden)
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-fstack-protector)
    add_compile_definitions(DEBUG)
else ()
    add_compile_options(-Os -g0)
endif ()
find_library(log-lib log)
add_library(ptyprocess SHARED main.cpp forkpty_compat.cpp)
target_link_libraries(ptyprocess "${log-lib}")
