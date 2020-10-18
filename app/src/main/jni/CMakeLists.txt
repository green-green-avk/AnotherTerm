cmake_minimum_required(VERSION 3.4.1)
set(EXECUTABLE_OUTPUT_PATH "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
add_executable(libtermsh.so main.cpp)
add_definitions(-fno-rtti)
add_definitions(-fno-exceptions)
add_definitions(-fvisibility=hidden)
# One can't simply pass a finally resolved applicationId here from a gradle script:
# https://stackoverflow.com/questions/64166557/android-studio-passing-a-finally-resolved-applicationid-to-the-native-code
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DAPPLICATION_ID=\"${APP_ID_BASE}${APP_ID_SDK}${APP_ID_TYPE}\"")
# ===
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -fstack-protector")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os -g0")
find_library(log-lib log)
target_link_libraries(libtermsh.so ${log-lib})
