cmake_minimum_required(VERSION 3.4.1)


add_library( # Sets the name of the library.
             gpuimage-library

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             yuv-decoder.c

            )

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

include_directories(
   # ${OpenCV_DIR}/include
    )

target_link_libraries(
    gpuimage-library
    ${log-lib}
)

