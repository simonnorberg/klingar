package net.simno.klingar.util;

import androidx.annotation.NonNull;

import java.util.Objects;

public final class Pair<A, B> {
  @NonNull public final A first;
  @NonNull public final B second;

  public Pair(@NonNull A first, @NonNull B second) {
    this.first = first;
    this.second = second;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return first.equals(pair.first) && second.equals(pair.second);
  }

  @Override public int hashCode() {
    return Objects.hash(first, second);
  }
}
