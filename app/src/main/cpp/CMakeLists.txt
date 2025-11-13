cmake_minimum_required(VERSION 3.4.1)

add_library(edgeview SHARED edgeview.cpp)

find_library(log-lib log)

target_link_libraries(edgeview ${log-lib})