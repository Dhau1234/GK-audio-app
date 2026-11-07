package com.hindigk.audio.data

object Blogs {
    val all = listOf(
        Blog(
            label = "Lucent GK",
            fullName = "Lucent Audio GK",
            host = "lucentaudiogk.blogspot.com"
        ),
        Blog(
            label = "मांजर",
            fullName = "मांजर भाग 2",
            host = "manjarpart2.blogspot.com"
        ),
        Blog(
            label = "विविध",
            fullName = "Miscellaneous",
            host = "miscellaneous578.blogspot.com"
        ),
        Blog(
            label = "झारखंड",
            fullName = "झारखंड दर्पण (ऑडियो)",
            host = "jharkhanddarpanaudio.blogspot.com"
        ),
        Blog(
            label = "सार संग्रह",
            fullName = "झारखंड सार संग्रह (हिंदी)",
            host = "jharkhandsaarsangrahhindi.blogspot.com"
        )
    )
}

data class Blog(
    val label: String,
    val fullName: String,
    val host: String
)
