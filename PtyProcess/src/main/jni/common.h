//
// Created by alex on 2/9/19.
//

#pragma once

#define HIDDEN __attribute__ ((visibility ("hidden")))

#define SIZEOFTBL(T) (sizeof(T)/sizeof((T)[0]))
