cmake_minimum_required(VERSION 3.4.1)


set(OpenCV_DIR "../opencv/sdk/native/jni")
FIND_PACKAGE(OpenCV REQUIRED)
add_library( # Sets the name of the library.
             cvUtils

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             cvUtils_jni.cc

            )

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

include_directories(
    ${OpenCV_DIR}/include
    )

target_link_libraries(
    cvUtils
    -ljnigraphics
    ${log-lib}
    ${OpenCV_LIBS}
)

