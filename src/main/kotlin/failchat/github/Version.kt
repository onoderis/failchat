package failchat.github

import java.util.regex.Pattern

class Version(val major: Int, val minor: Int, val micro: Int) : Comparable<Version> {

    companion object {
        val versionPattern: Pattern = Pattern.compile("""v(\d+)\.(\d+)\.(\d+)""")

        fun parse(stringVersion: String): Version {
            val matcher = versionPattern.matcher(stringVersion)
            if (!matcher.matches()) throw IllegalArgumentException("Invalid version format: '$stringVersion'")
            return Version(matcher.group(1).toInt(), matcher.group(2).toInt(), matcher.group(3).toInt())
        }
    }

    override fun compareTo(other: Version): Int {
        var comparison: Int = major.compareTo(other.major)
        if (comparison != 0) return comparison

        comparison = minor.compareTo(other.minor)
        if (comparison != 0) return comparison

        return micro.compareTo(other.micro)
    }

    override fun toString() = "v$major.$minor.$micro"

}
