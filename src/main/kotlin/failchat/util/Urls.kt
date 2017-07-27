package failchat.util

import java.util.regex.Pattern

/**
 * URL pattern.
 *
 * Capture groups:
 * 1. protocol (http, https, ftp, ftps)
 * 2. "www."
 * 3. short url
 * 4. domain
 */
val urlPattern: Pattern = Pattern.compile("""\b(https?|ftps?)://(w{3}\.)?(([-\w\d+&@#%?=~_|!:,.;]+)[/\S]*)""")
