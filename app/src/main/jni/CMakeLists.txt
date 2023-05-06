cmake_minimum_required(VERSION 3.22.1)
project(termsh)
# One can't simply pass a finally resolved applicationId here from a gradle script:
# https://stackoverflow.com/questions/64166557/android-studio-passing-a-finally-resolved-applicationid-to-the-native-code
add_compile_definitions(APPLICATION_ID="${APP_ID_BASE}${APP_ID_SDK}${APP_ID_TYPE}")
# ===
add_compile_options(-std=c++17 -fno-rtti -fno-exceptions -fvisibility=hidden)
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    add_compile_options(-fstack-protector)
else ()
    add_compile_options(-Os -g0)
endif ()
find_library(log-lib log)
add_executable(libtermsh.so main.cpp)
set_target_properties(libtermsh.so PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${CMAKE_LIBRARY_OUTPUT_DIRECTORY}")
target_link_libraries(libtermsh.so "${log-lib}")
