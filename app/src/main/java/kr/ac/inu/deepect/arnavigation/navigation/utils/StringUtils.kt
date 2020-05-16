package kr.ac.inu.deepect.arnavigation.navigation.utils

class StringUtils {

    companion object {
        fun join(delim: Char, strings : Array<String>): String {
            if (strings.size == 0) {
                return ""
            }
            val sb = StringBuilder()
            sb.append(strings[0])
            for (i in 1 until strings.size) {
                if (strings[i] != null && !strings[i]!!.isEmpty()) {
                    sb.append(delim + strings[i]!!)
                }
            }
            return sb.toString()
        }
    }
}