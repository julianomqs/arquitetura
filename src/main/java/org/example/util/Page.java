package org.example.util;

import java.util.List;

public record Page<T>(List<T> result, long total) {
}
