package me.leaver.rexpocket.data

/**
 * A monospace family the terminal can download: four faces from one pinned commit of the font's
 * own repository, each checked against its SHA-256. Nothing is bundled in the APK.
 */
internal data class TerminalFamily(
    val id: String,
    val label: String,
    val directory: String,
    /** Raw-file base URL at a pinned commit; each asset's name is appended. */
    val source: String,
    val regular: String,
    val bold: String,
    val italic: String,
    val boldItalic: String,
    val assets: List<FontAsset>,
    /** A ceiling per file, so a wrong URL cannot fill the disk; Nerd Font builds are ~2.6 MB. */
    val maxFileBytes: Long,
    /** The download as the picker states it. */
    val size: String,
)

private const val MIB = 1_048_576L

internal val TERMINAL_FAMILIES = listOf(
    TerminalFamily(
        id = "jetbrains",
        label = "JetBrains Mono",
        // The directory the single-font version saved into, so a family downloaded then still counts.
        directory = "jetbrains-mono-v1",
        source = "https://raw.githubusercontent.com/JetBrains/JetBrainsMono/19371302b95d218af43299bce79ddbddd0bc364d/fonts/ttf",
        regular = "JetBrainsMono-Regular.ttf", bold = "JetBrainsMono-Bold.ttf", italic = "JetBrainsMono-Italic.ttf", boldItalic = "JetBrainsMono-BoldItalic.ttf",
        assets = listOf(
            FontAsset("JetBrainsMono-Regular.ttf", "e6fd0d7e91550b3ed2b735d4312474362c4716edc4fc0577a0f61ed782d5aed1"),
            FontAsset("JetBrainsMono-Bold.ttf", "d22c4f3821d725eb01210d278d95dfcfcaadc34699a06658d47c8a5cc5830ada"),
            FontAsset("JetBrainsMono-Italic.ttf", "6dc8e1322d4b2013b64e277bf66abc6748207c46a590471d4296876c2c235d0c"),
            FontAsset("JetBrainsMono-BoldItalic.ttf", "cec9b489af5c98a94cb8d8d8708ef1640cca0f296b207de1f50b2dd95e967bdf"),
        ),
        maxFileBytes = MIB,
        size = "1.1 MB",
    ),
    TerminalFamily(
        id = "meslo",
        label = "Meslo LG NF",
        directory = "meslo-lgs-nf-v1",
        source = "https://raw.githubusercontent.com/romkatv/powerlevel10k-media/145eb9fbc2f42ee408dacd9b22d8e6e0e553f83d",
        regular = "MesloLGS NF Regular.ttf", bold = "MesloLGS NF Bold.ttf", italic = "MesloLGS NF Italic.ttf", boldItalic = "MesloLGS NF Bold Italic.ttf",
        assets = listOf(
            FontAsset("MesloLGS NF Regular.ttf", "d97946186e97f8d7c0139e8983abf40a1d2d086924f2c5dbf1c29bd8f2c6e57d"),
            FontAsset("MesloLGS NF Bold.ttf", "b6c0199cf7c7483c8343ea020658925e6de0aeb318b89908152fcb4d19226003"),
            FontAsset("MesloLGS NF Italic.ttf", "6f357bcbe2597704e157a915625928bca38364a89c22a4ac36e7a116dcd392ef"),
            FontAsset("MesloLGS NF Bold Italic.ttf", "56b4131adecec052c4b324efb818dd326d586dbc316fc68f98f1cae2eb8d1220"),
        ),
        maxFileBytes = 3 * MIB,
        size = "10 MB",
    ),
    TerminalFamily(
        id = "hack",
        label = "Hack",
        directory = "hack-v1",
        source = "https://raw.githubusercontent.com/source-foundry/Hack/a737c121cabb337fdfe655d8c7304729f351e30f/build/ttf",
        regular = "Hack-Regular.ttf", bold = "Hack-Bold.ttf", italic = "Hack-Italic.ttf", boldItalic = "Hack-BoldItalic.ttf",
        assets = listOf(
            FontAsset("Hack-Regular.ttf", "15f55cc0c85a2988d2b4b3a8cdb5d77fdfbaf319e1bb5309d725db9818fb7125"),
            FontAsset("Hack-Bold.ttf", "5bbf531eff7f8a0c2559c9a0656718e2828a012a9b1f60b5f54006d59a4de8d4"),
            FontAsset("Hack-Italic.ttf", "096fb67a2b85f3c866e9cb3e965b27c2c10b977315f4d3d7f095674be35091c1"),
            FontAsset("Hack-BoldItalic.ttf", "64f74a079700b7dfe128551a1e28875d5ba980971e55f5e0f0596e37bdc6a6bc"),
        ),
        maxFileBytes = MIB,
        size = "1.2 MB",
    ),
    TerminalFamily(
        id = "plex",
        label = "IBM Plex Mono",
        directory = "ibm-plex-mono-v1",
        source = "https://raw.githubusercontent.com/IBM/plex/bc6a7e4b37634eb79785d59781893db63ee3edcb/packages/plex-mono/fonts/complete/ttf",
        regular = "IBMPlexMono-Regular.ttf", bold = "IBMPlexMono-Bold.ttf", italic = "IBMPlexMono-Italic.ttf", boldItalic = "IBMPlexMono-BoldItalic.ttf",
        assets = listOf(
            FontAsset("IBMPlexMono-Regular.ttf", "7c6fbddca4b700be918f5f6183d9bd4464fa427fe435f0b480d77fe2bb8c5a43"),
            FontAsset("IBMPlexMono-Bold.ttf", "74e5eedcfa4596497d34e19023cabdabd3a8c852b903007a5654a59591a72ffb"),
            FontAsset("IBMPlexMono-Italic.ttf", "a55368deb953f594ac18336a1029009ed820e2d9abbb57ad601f61e22bec072b"),
            FontAsset("IBMPlexMono-BoldItalic.ttf", "c8b00068fd2fd69bbcf62059148623f09d19e9ada8bf9e8caf1aedd01347046a"),
        ),
        maxFileBytes = MIB,
        size = "0.7 MB",
    ),
    TerminalFamily(
        id = "source-code-pro",
        label = "Source Code Pro",
        directory = "source-code-pro-v1",
        source = "https://raw.githubusercontent.com/adobe-fonts/source-code-pro/803b7e23ec97ae58b6232ea76519a76d428ba268/TTF",
        regular = "SourceCodePro-Regular.ttf", bold = "SourceCodePro-Bold.ttf", italic = "SourceCodePro-It.ttf", boldItalic = "SourceCodePro-BoldIt.ttf",
        assets = listOf(
            FontAsset("SourceCodePro-Regular.ttf", "74bd80d3e42a08517cd7e1108ba3d86f2da29ac0f3065be95e0357956ab9db37"),
            FontAsset("SourceCodePro-Bold.ttf", "b2095e0d657e6d28dc32444a9dacabab0c9241d0bf39d96371756cc9bdbc3a5f"),
            FontAsset("SourceCodePro-It.ttf", "9c9e0f4d016210a3c5bdfba5262637c5b26ddff4ccc382ebbc781de5961d0042"),
            FontAsset("SourceCodePro-BoldIt.ttf", "1b49d9304012bf8db9e5dd4104183d5c122c445d0570a2259125f71977595b90"),
        ),
        maxFileBytes = MIB,
        size = "0.75 MB",
    ),
    TerminalFamily(
        id = "ubuntu",
        label = "Ubuntu Mono",
        directory = "ubuntu-mono-v1",
        source = "https://raw.githubusercontent.com/google/fonts/7af0d90b4d532ffa4b041937b560c6df4e7b1e95/ufl/ubuntumono",
        regular = "UbuntuMono-Regular.ttf", bold = "UbuntuMono-Bold.ttf", italic = "UbuntuMono-Italic.ttf", boldItalic = "UbuntuMono-BoldItalic.ttf",
        assets = listOf(
            FontAsset("UbuntuMono-Regular.ttf", "b35dd9d2131d5d83a9b87fe9ad22c6288fa3d17688d43302c14da29812417d63"),
            FontAsset("UbuntuMono-Bold.ttf", "11f15c3a6bbd998a8695fdefb3475931c3789aa035d7546f2efe78e83b352f6b"),
            FontAsset("UbuntuMono-Italic.ttf", "960b2bc286c2ff7d49073303858c65e1fc9013c17a971b61123b02c39454ef75"),
            FontAsset("UbuntuMono-BoldItalic.ttf", "bd255784bb87b5c41513a12a86f0f9cf061bce4e8256d3bfe7234611002e8f48"),
        ),
        maxFileBytes = MIB,
        size = "0.8 MB",
    ),
)

internal fun terminalFamily(id: String?): TerminalFamily? = TERMINAL_FAMILIES.firstOrNull { it.id == id }
