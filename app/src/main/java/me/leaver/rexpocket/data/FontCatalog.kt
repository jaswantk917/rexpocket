package me.leaver.rexpocket.data

internal const val SYSTEM_FONT = "system"

internal data class TerminalFont(
    val id: String,
    val name: String,
    val source: String,
    val assets: List<FontAsset>,
    val downloadMb: Int,
    val nerd: Boolean = false,
    /** Where a family saved before this catalog existed, so it is not downloaded again. */
    val cache: String? = null,
) {
    val cacheKey: String get() = cache ?: if (id == "jetbrains-mono") "jetbrains-mono-v1" else "$id-v3.4.0"
}

/** Immutable upstream revisions and hashes: never accept an unverified font download. */
internal object FontCatalog {
    private const val NERD = "https://raw.githubusercontent.com/ryanoasis/nerd-fonts/fa7b859994228a9c8759f99c55a8d31ee92a1b5e/patched-fonts"
    val fonts = listOf(
        TerminalFont("jetbrains-mono", "JetBrains Mono",
            "https://raw.githubusercontent.com/JetBrains/JetBrainsMono/19371302b95d218af43299bce79ddbddd0bc364d/fonts/ttf",
            listOf(
                FontAsset("JetBrainsMono-Regular.ttf", "e6fd0d7e91550b3ed2b735d4312474362c4716edc4fc0577a0f61ed782d5aed1"),
                FontAsset("JetBrainsMono-Bold.ttf", "d22c4f3821d725eb01210d278d95dfcfcaadc34699a06658d47c8a5cc5830ada", weight = 700),
                FontAsset("JetBrainsMono-Italic.ttf", "6dc8e1322d4b2013b64e277bf66abc6748207c46a590471d4296876c2c235d0c", italic = true),
                FontAsset("JetBrainsMono-BoldItalic.ttf", "cec9b489af5c98a94cb8d8d8708ef1640cca0f296b207de1f50b2dd95e967bdf", weight = 700, italic = true),
            ), 1),
        TerminalFont("jetbrains-nerd", "JetBrains Mono Nerd Font", "$NERD/JetBrainsMono/Ligatures", listOf(
            FontAsset("JetBrainsMonoNerdFontMono-Regular.ttf", "f01031f40e48dc29e1112e6b0b0450a2c6cd097f3f35cfff05c55cb311f8034c", "Regular"),
            FontAsset("JetBrainsMonoNerdFontMono-Bold.ttf", "5bdd4a873f3cd32f882d2c55545089123926e27707d5880fc9eaf84eb01b6686", "Bold", 700),
            FontAsset("JetBrainsMonoNerdFontMono-Italic.ttf", "ccd88b36d325e6a905edc8dd3f2522718d9690d9bed3fbb4684c7e746c34f846", "Italic", italic = true),
            FontAsset("JetBrainsMonoNerdFontMono-BoldItalic.ttf", "d931df2928b3216892d35980cddcad9edade1b9c9cd2e09a6c2937139f474742", "BoldItalic", 700, true),
        ), 10, nerd = true),
        TerminalFont("fira-code-nerd", "Fira Code Nerd Font", "$NERD/FiraCode", listOf(
            FontAsset("FiraCodeNerdFontMono-Regular.ttf", "ad88c69cb6a497db9f2714e4b414817aabbee621484a1560bfdb3fd73abdd564", "Regular"),
            FontAsset("FiraCodeNerdFontMono-Bold.ttf", "bd014a21f3cd64203dd0850437091d716e929a4518b0e5848d6d3ad6af12b21d", "Bold", 700),
        ), 5, nerd = true),
        TerminalFont("hack-nerd", "Hack Nerd Font", "$NERD/Hack", listOf(
            FontAsset("HackNerdFontMono-Regular.ttf", "03e60d3c1a9f8bef4e1f78836f80aacb9ec005260a6b094f5bfc10043bb115ab", "Regular"),
            FontAsset("HackNerdFontMono-Bold.ttf", "711084fdea9f9eb4e5dbca372a19e6a5af996fc88bfce55918eeef560f0f6722", "Bold", 700),
            FontAsset("HackNerdFontMono-Italic.ttf", "86c6e1b14e2cb02ac8041269c53dac3673c70ff58375d1aafe0ecff8087f8126", "Italic", italic = true),
            FontAsset("HackNerdFontMono-BoldItalic.ttf", "82fca6ff9e87bc65b6abb1bbde1e3884fa9418bfb14f15b9db2d3274b87bf44e", "BoldItalic", 700, true),
        ), 11, nerd = true),
        TerminalFont("meslo-nerd", "Meslo LGL Nerd Font", "$NERD/Meslo/L", listOf(
            FontAsset("MesloLGLNerdFontMono-Regular.ttf", "de03898cc08f9922bc69c98b24c54c69ee0f2e65eabb399a29e2396912a56a51", "Regular"),
            FontAsset("MesloLGLNerdFontMono-Bold.ttf", "175f82836af6bed1fb9b1f1f76d6cf91007b4fe54d4daebb282233058580196e", "Bold", 700),
            FontAsset("MesloLGLNerdFontMono-Italic.ttf", "abc8011f5060f1315146b4a55a3aa7700fd53d07028c243bc2cfe8379bcacc2d", "Italic", italic = true),
            FontAsset("MesloLGLNerdFontMono-BoldItalic.ttf", "51ae8c922457fd0c87a88657afe04fcfbd25585f76a4aefe1b51f50e6dc4700b", "Bold-Italic", 700, true),
        ), 11, nerd = true),
        TerminalFont("meslo", "Meslo LG NF", "https://raw.githubusercontent.com/romkatv/powerlevel10k-media/145eb9fbc2f42ee408dacd9b22d8e6e0e553f83d", listOf(
            FontAsset("MesloLGS NF Regular.ttf", "d97946186e97f8d7c0139e8983abf40a1d2d086924f2c5dbf1c29bd8f2c6e57d"),
            FontAsset("MesloLGS NF Bold.ttf", "b6c0199cf7c7483c8343ea020658925e6de0aeb318b89908152fcb4d19226003", weight = 700),
            FontAsset("MesloLGS NF Italic.ttf", "6f357bcbe2597704e157a915625928bca38364a89c22a4ac36e7a116dcd392ef", italic = true),
            FontAsset("MesloLGS NF Bold Italic.ttf", "56b4131adecec052c4b324efb818dd326d586dbc316fc68f98f1cae2eb8d1220", weight = 700, italic = true),
        ), 10, nerd = true, cache = "meslo-lgs-nf-v1"),
        TerminalFont("hack", "Hack", "https://raw.githubusercontent.com/source-foundry/Hack/a737c121cabb337fdfe655d8c7304729f351e30f/build/ttf", listOf(
            FontAsset("Hack-Regular.ttf", "15f55cc0c85a2988d2b4b3a8cdb5d77fdfbaf319e1bb5309d725db9818fb7125"),
            FontAsset("Hack-Bold.ttf", "5bbf531eff7f8a0c2559c9a0656718e2828a012a9b1f60b5f54006d59a4de8d4", weight = 700),
            FontAsset("Hack-Italic.ttf", "096fb67a2b85f3c866e9cb3e965b27c2c10b977315f4d3d7f095674be35091c1", italic = true),
            FontAsset("Hack-BoldItalic.ttf", "64f74a079700b7dfe128551a1e28875d5ba980971e55f5e0f0596e37bdc6a6bc", weight = 700, italic = true),
        ), 1, cache = "hack-v1"),
        TerminalFont("plex", "IBM Plex Mono", "https://raw.githubusercontent.com/IBM/plex/bc6a7e4b37634eb79785d59781893db63ee3edcb/packages/plex-mono/fonts/complete/ttf", listOf(
            FontAsset("IBMPlexMono-Regular.ttf", "7c6fbddca4b700be918f5f6183d9bd4464fa427fe435f0b480d77fe2bb8c5a43"),
            FontAsset("IBMPlexMono-Bold.ttf", "74e5eedcfa4596497d34e19023cabdabd3a8c852b903007a5654a59591a72ffb", weight = 700),
            FontAsset("IBMPlexMono-Italic.ttf", "a55368deb953f594ac18336a1029009ed820e2d9abbb57ad601f61e22bec072b", italic = true),
            FontAsset("IBMPlexMono-BoldItalic.ttf", "c8b00068fd2fd69bbcf62059148623f09d19e9ada8bf9e8caf1aedd01347046a", weight = 700, italic = true),
        ), 1, cache = "ibm-plex-mono-v1"),
        TerminalFont("source-code-pro", "Source Code Pro", "https://raw.githubusercontent.com/adobe-fonts/source-code-pro/803b7e23ec97ae58b6232ea76519a76d428ba268/TTF", listOf(
            FontAsset("SourceCodePro-Regular.ttf", "74bd80d3e42a08517cd7e1108ba3d86f2da29ac0f3065be95e0357956ab9db37"),
            FontAsset("SourceCodePro-Bold.ttf", "b2095e0d657e6d28dc32444a9dacabab0c9241d0bf39d96371756cc9bdbc3a5f", weight = 700),
            FontAsset("SourceCodePro-It.ttf", "9c9e0f4d016210a3c5bdfba5262637c5b26ddff4ccc382ebbc781de5961d0042", italic = true),
            FontAsset("SourceCodePro-BoldIt.ttf", "1b49d9304012bf8db9e5dd4104183d5c122c445d0570a2259125f71977595b90", weight = 700, italic = true),
        ), 1, cache = "source-code-pro-v1"),
        TerminalFont("ubuntu", "Ubuntu Mono", "https://raw.githubusercontent.com/google/fonts/7af0d90b4d532ffa4b041937b560c6df4e7b1e95/ufl/ubuntumono", listOf(
            FontAsset("UbuntuMono-Regular.ttf", "b35dd9d2131d5d83a9b87fe9ad22c6288fa3d17688d43302c14da29812417d63"),
            FontAsset("UbuntuMono-Bold.ttf", "11f15c3a6bbd998a8695fdefb3475931c3789aa035d7546f2efe78e83b352f6b", weight = 700),
            FontAsset("UbuntuMono-Italic.ttf", "960b2bc286c2ff7d49073303858c65e1fc9013c17a971b61123b02c39454ef75", italic = true),
            FontAsset("UbuntuMono-BoldItalic.ttf", "bd255784bb87b5c41513a12a86f0f9cf061bce4e8256d3bfe7234611002e8f48", weight = 700, italic = true),
        ), 1, cache = "ubuntu-mono-v1"),
    )

    fun find(id: String): TerminalFont? = fonts.find { it.id == id }
}
