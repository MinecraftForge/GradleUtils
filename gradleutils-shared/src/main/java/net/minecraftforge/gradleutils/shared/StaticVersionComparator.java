/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.minecraftforge.gradleutils.shared;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// https://github.com/gradle/gradle/blob/v9.0.0/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/ivyresolve/strategy/StaticVersionComparator.java
@SuppressWarnings("ObjectInstantiationInEqualsHashCode")
class StaticVersionComparator implements Comparator<String>, Serializable {
    private static final @Serial long serialVersionUID = 6082715929370009879L;
    static final StaticVersionComparator INSTANCE = new StaticVersionComparator();

    static final Map<String, Integer> SPECIAL_MEANINGS = Map.of(
        "dev", -1,
        "rc", 1,
        "snapshot", 2,
        "final", 3, "ga", 4, "release", 5,
        "sp", 6);

    @Override
    public int compare(String o1, String o2) {
        return compareNow(o1, o2);
    }

    static int compareNow(String o1, String o2) {
        return compare(Version.parse(o1), Version.parse(o2));
    }

    /**
     * Compares 2 versions. Algorithm is inspired by PHP version_compare one.
     */
    private static int compare(Version version1, Version version2) {
        if (version1.equals(version2)) {
            return 0;
        }

        String[] parts1 = version1.getParts();
        String[] parts2 = version2.getParts();
        Long[] numericParts1 = version1.getNumericParts();
        Long[] numericParts2 = version2.getNumericParts();

        int i = 0;
        for (; i < parts1.length && i < parts2.length; i++) {
            String part1 = parts1[i];
            String part2 = parts2[i];

            Long numericPart1 = numericParts1[i];
            Long numericPart2 = numericParts2[i];

            boolean is1Number = numericPart1 != null;
            boolean is2Number = numericPart2 != null;

            if (part1.equals(part2)) {
                continue;
            }
            if (is1Number && !is2Number) {
                return 1;
            }
            if (is2Number && !is1Number) {
                return -1;
            }
            if (is1Number && is2Number) {
                int result = numericPart1.compareTo(numericPart2);
                if (result == 0) {
                    continue;
                }
                return result;
            }
            // both are strings, we compare them taking into account special meaning
            Integer sm1 = SPECIAL_MEANINGS.get(part1.toLowerCase(Locale.US));
            Integer sm2 = SPECIAL_MEANINGS.get(part2.toLowerCase(Locale.US));
            if (sm1 != null) {
                sm2 = sm2 == null ? 0 : sm2;
                return sm1 - sm2;
            }
            if (sm2 != null) {
                return -sm2;
            }
            return part1.compareTo(part2);
        }
        if (i < parts1.length) {
            return numericParts1[i] == null ? -1 : 1;
        }
        if (i < parts2.length) {
            return numericParts2[i] == null ? 1 : -1;
        }

        return 0;
    }

    // https://github.com/gradle/gradle/blob/v9.0.0/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/ivyresolve/strategy/Version.java
    private interface Version {
        /**
         * Returns the original {@link String} representation of the version.
         */
        String getSource();

        /**
         * Returns all the parts of this version. e.g. 1.2.3 returns [1,2,3] or 1.2-beta4 returns [1,2,beta,4].
         */
        String[] getParts();

        /**
         * Returns all the numeric parts of this version as {@link Long}, with nulls in non-numeric positions. eg. 1.2.3 returns [1,2,3] or 1.2-beta4 returns [1,2,null,4].
         */
        Long[] getNumericParts();

        /**
         * Returns the base version for this version, which removes any qualifiers. Generally this is the first '.' separated parts of this version.
         * e.g. 1.2.3-beta-4 returns 1.2.3, or 7.0.12beta5 returns 7.0.12.
         */
        Version getBaseVersion();

        /**
         * Returns true if this version is qualified in any way. For example, 1.2.3 is not qualified, 1.2-beta-3 is.
         */
        boolean isQualified();

        // https://github.com/gradle/gradle/blob/328772c6bae126949610a8beb59cb227ee580241/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/ivyresolve/strategy/VersionParser.java#L41-L88
        static Version parse(String original) {
            List<String> parts = new ArrayList<>();
            boolean digit = false;
            int startPart = 0;
            int pos = 0;
            int endBase = 0;
            int endBaseStr = 0;
            for (; pos < original.length(); pos++) {
                char ch = original.charAt(pos);
                if (ch == '.' || ch == '_' || ch == '-' || ch == '+') {
                    parts.add(original.substring(startPart, pos));
                    startPart = pos + 1;
                    digit = false;
                    if (ch != '.' && endBaseStr == 0) {
                        endBase = parts.size();
                        endBaseStr = pos;
                    }
                } else if (ch >= '0' && ch <= '9') {
                    if (!digit && pos > startPart) {
                        if (endBaseStr == 0) {
                            endBase = parts.size() + 1;
                            endBaseStr = pos;
                        }
                        parts.add(original.substring(startPart, pos));
                        startPart = pos;
                    }
                    digit = true;
                } else {
                    if (digit) {
                        if (endBaseStr == 0) {
                            endBase = parts.size() + 1;
                            endBaseStr = pos;
                        }
                        parts.add(original.substring(startPart, pos));
                        startPart = pos;
                    }
                    digit = false;
                }
            }
            if (pos > startPart) {
                parts.add(original.substring(startPart, pos));
            }
            DefaultVersion base = null;
            if (endBaseStr > 0) {
                base = new DefaultVersion(original.substring(0, endBaseStr), parts.subList(0, endBase), null);
            }
            return new DefaultVersion(original, parts, base);
        }
    }

    // https://github.com/gradle/gradle/blob/328772c6bae126949610a8beb59cb227ee580241/platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/ivyresolve/strategy/VersionParser.java#L90-L152
    private static class DefaultVersion implements Version {
        private final String source;
        private final String[] parts;
        private final Long[] numericParts;
        private final DefaultVersion baseVersion;

        public DefaultVersion(String source, List<String> parts, DefaultVersion baseVersion) {
            this.source = source;
            this.parts = parts.toArray(new String[0]);
            this.numericParts = new Long[this.parts.length];
            for (int i = 0; i < parts.size(); i++) {
                Long part = null;
                try {
                    part = Long.parseLong(this.parts[i]);
                } catch (NumberFormatException ignored) { }
                this.numericParts[i] = part;
            }
            this.baseVersion = baseVersion == null ? this : baseVersion;
        }

        @Override
        public String toString() {
            return source;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }
            DefaultVersion other = (DefaultVersion) obj;
            return source.equals(other.source);
        }

        @Override
        public int hashCode() {
            return source.hashCode();
        }

        @Override
        public boolean isQualified() {
            return baseVersion != this;
        }

        @Override
        public Version getBaseVersion() {
            return baseVersion;
        }

        @Override
        public String[] getParts() {
            return parts;
        }

        @Override
        public Long[] getNumericParts() {
            return numericParts;
        }

        @Override
        public String getSource() {
            return source;
        }
    }
}
