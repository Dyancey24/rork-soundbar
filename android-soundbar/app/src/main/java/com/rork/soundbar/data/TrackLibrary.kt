package com.rork.soundbar.data

/**
 * A sample listening catalogue. Tracks are grouped by genre so a blend can be
 * poured from whatever genres the user mixed, in the proportions they chose.
 */
object TrackLibrary {

    private val tracks: Map<String, List<Track>> = mapOf(
        "jazz" to listOf(
            Track("Blue in Green", "Miles Davis", 337, "jazz"),
            Track("Slide", "Bill Evans", 372, "jazz"),
            Track("Foam", "GoGo Penguin", 228, "jazz"),
            Track("Naima", "John Coltrane", 265, "jazz"),
            Track("Ceora", "Lee Morgan", 385, "jazz"),
            Track("Peace Piece", "Bill Evans", 402, "jazz"),
            Track("Lonely Woman", "Ornette Coleman", 297, "jazz")
        ),
        "house" to listOf(
            Track("Glass Floor", "Kerri Chandler", 391, "house"),
            Track("Late Night Tuff Guy", "Larry Heard", 348, "house"),
            Track("Marble Hall", "Floating Points", 412, "house"),
            Track("Can't Do Without You", "Caribou", 271, "house"),
            Track("Your Love", "Frankie Knuckles", 356, "house"),
            Track("Sundial", "Move D", 380, "house")
        ),
        "hiphop" to listOf(
            Track("Nautilus", "Pete Rock", 244, "hiphop"),
            Track("Accordion", "Madvillain", 118, "hiphop"),
            Track("Vessels", "Roc Marciano", 202, "hiphop"),
            Track("Rain Check", "Little Simz", 234, "hiphop"),
            Track("Brick Body", "Open Mike Eagle", 199, "hiphop"),
            Track("Nights", "Frank Ocean", 307, "hiphop")
        ),
        "country" to listOf(
            Track("Pancho and Lefty", "Townes Van Zandt", 269, "country"),
            Track("Dreams", "Kacey Musgraves", 213, "country"),
            Track("Long Violent History", "Tyler Childers", 256, "country"),
            Track("Blue Bayou", "Linda Ronstadt", 232, "country"),
            Track("Highway Patrolman", "Bruce Springsteen", 322, "country")
        ),
        "metal" to listOf(
            Track("Bloodstone", "Judas Priest", 261, "metal"),
            Track("Gargoyle", "Deafheaven", 401, "metal"),
            Track("Ash Column", "Russian Circles", 355, "metal"),
            Track("Aerials", "System of a Down", 235, "metal"),
            Track("Copper Rise", "Elder", 448, "metal")
        ),
        "soul" to listOf(
            Track("A Change Is Gonna Come", "Sam Cooke", 191, "soul"),
            Track("Inner City Blues", "Marvin Gaye", 315, "soul"),
            Track("Tired of Being Alone", "Al Green", 168, "soul"),
            Track("Golden Hour", "Charles Bradley", 244, "soul"),
            Track("Sitting on Top", "Sharon Jones", 208, "soul")
        ),
        "funk" to listOf(
            Track("Cissy Strut", "The Meters", 185, "funk"),
            Track("Flash Light", "Parliament", 283, "funk"),
            Track("Rock Creek Park", "The Blackbyrds", 216, "funk"),
            Track("Cold Sweat", "James Brown", 175, "funk"),
            Track("Nautilus Grease", "Bobbi Humphrey", 260, "funk")
        ),
        "classical" to listOf(
            Track("Gymnopédie No. 1", "Erik Satie", 210, "classical"),
            Track("Spiegel im Spiegel", "Arvo Pärt", 552, "classical"),
            Track("Nocturne in C# minor", "Frédéric Chopin", 244, "classical"),
            Track("The Lark Ascending", "Ralph Vaughan Williams", 890, "classical")
        ),
        "dreampop" to listOf(
            Track("Cherry-coloured Funk", "Cocteau Twins", 271, "dreampop"),
            Track("Space Song", "Beach House", 320, "dreampop"),
            Track("Sight of You", "Pale Saints", 224, "dreampop"),
            Track("Bloom", "Mazzy Star", 258, "dreampop"),
            Track("Slow Glass", "Hatchie", 239, "dreampop")
        ),
        "bossanova" to listOf(
            Track("Corcovado", "João Gilberto", 142, "bossanova"),
            Track("Águas de Março", "Elis Regina", 216, "bossanova"),
            Track("Berimbau", "Astrud Gilberto", 187, "bossanova"),
            Track("Wave", "Antônio Carlos Jobim", 172, "bossanova")
        ),
        "triphop" to listOf(
            Track("Teardrop", "Massive Attack", 330, "triphop"),
            Track("Glory Box", "Portishead", 305, "triphop"),
            Track("Angel", "Tricky", 271, "triphop"),
            Track("Dusted", "Leftfield", 289, "triphop"),
            Track("Hell Is Round the Corner", "Tricky", 226, "triphop")
        ),
        "gospel" to listOf(
            Track("Take My Hand", "Mahalia Jackson", 214, "gospel"),
            Track("Wholy Holy", "Aretha Franklin", 291, "gospel"),
            Track("Oh Happy Day", "Edwin Hawkins Singers", 318, "gospel"),
            Track("Rain Down", "The Clark Sisters", 265, "gospel")
        ),
        "ambient" to listOf(
            Track("An Ending (Ascent)", "Brian Eno", 264, "ambient"),
            Track("Substrata", "Biosphere", 372, "ambient"),
            Track("Avril 14th", "Aphex Twin", 125, "ambient"),
            Track("Kompajn", "Ólafur Arnalds", 288, "ambient"),
            Track("Drift Layer", "Grouper", 341, "ambient")
        ),
        "shoegaze" to listOf(
            Track("Only Shallow", "My Bloody Valentine", 257, "shoegaze"),
            Track("Vapour Trail", "Ride", 265, "shoegaze"),
            Track("Alison", "Slowdive", 227, "shoegaze"),
            Track("Sometimes", "Whirr", 312, "shoegaze")
        ),
        "postpunk" to listOf(
            Track("Disorder", "Joy Division", 205, "postpunk"),
            Track("A Forest", "The Cure", 352, "postpunk"),
            Track("Marquee Moon", "Television", 589, "postpunk"),
            Track("Iron Curtain", "Protomartyr", 241, "postpunk")
        ),
        "disco" to listOf(
            Track("I Feel Love", "Donna Summer", 349, "disco"),
            Track("Lady Marmalade", "Labelle", 191, "disco"),
            Track("Spacer", "Sheila & B. Devotion", 401, "disco"),
            Track("Love Sensation", "Loleatta Holloway", 372, "disco")
        ),
        "lofi" to listOf(
            Track("Sunset Drive", "Nujabes", 218, "lofi"),
            Track("Coffee Shop", "Idealism", 154, "lofi"),
            Track("Paper Cranes", "L'indécis", 173, "lofi"),
            Track("Slow Morning", "Bcalm", 162, "lofi"),
            Track("Dusty Tape", "Kupla", 189, "lofi")
        ),
        "indiefolk" to listOf(
            Track("Holocene", "Bon Iver", 337, "indiefolk"),
            Track("The Wolves", "Bon Iver", 320, "indiefolk"),
            Track("Orchards", "Fleet Foxes", 285, "indiefolk"),
            Track("Motion Sickness", "Phoebe Bridgers", 240, "indiefolk")
        ),
        "rnb" to listOf(
            Track("Galaxy", "Erykah Badu", 243, "rnb"),
            Track("Cranes in the Sky", "Solange", 251, "rnb"),
            Track("Untitled (How Does It Feel)", "D'Angelo", 424, "rnb"),
            Track("Silk", "Sade", 262, "rnb")
        ),
        "afrobeat" to listOf(
            Track("Water No Get Enemy", "Fela Kuti", 654, "afrobeat"),
            Track("Ye Ye De Smell", "Tony Allen", 412, "afrobeat"),
            Track("Ojuelegba", "Wizkid", 209, "afrobeat"),
            Track("Zombie", "Fela Kuti", 736, "afrobeat")
        ),
        "synthwave" to listOf(
            Track("Nightcall", "Kavinsky", 258, "synthwave"),
            Track("Chrome Coast", "The Midnight", 294, "synthwave"),
            Track("Turbo Killer", "Carpenter Brut", 245, "synthwave"),
            Track("Neon Rain", "Com Truise", 231, "synthwave")
        ),
        "reggae" to listOf(
            Track("King Tubby Meets Rockers", "Augustus Pablo", 224, "reggae"),
            Track("Marcus Garvey", "Burning Spear", 233, "reggae"),
            Track("Tidal Dub", "Scientist", 198, "reggae"),
            Track("Satta Massagana", "The Abyssinians", 261, "reggae")
        ),
        "dnb" to listOf(
            Track("Inner City Life", "Goldie", 361, "dnb"),
            Track("Pulp Fiction", "Alex Reece", 322, "dnb"),
            Track("Static Signal", "Calibre", 348, "dnb"),
            Track("Brown Paper Bag", "Roni Size", 349, "dnb")
        ),
        "psychrock" to listOf(
            Track("The Wind", "Khruangbin", 232, "psychrock"),
            Track("Kaleidoscope", "Tame Impala", 302, "psychrock"),
            Track("Interstellar Overdrive", "Pink Floyd", 581, "psychrock"),
            Track("Fuzz Bloom", "King Gizzard", 266, "psychrock")
        ),
        "neosoul" to listOf(
            Track("Brown Sugar", "D'Angelo", 274, "neosoul"),
            Track("Plush Velvet", "Hiatus Kaiyote", 289, "neosoul"),
            Track("Bag Lady", "Erykah Badu", 348, "neosoul"),
            Track("Ain't No Sunshine", "José James", 231, "neosoul")
        ),
        "garage" to listOf(
            Track("Rust", "Burial", 386, "garage"),
            Track("Sweet Like Chocolate", "Shanks & Bigfoot", 214, "garage"),
            Track("Skittering", "Zed Bias", 259, "garage"),
            Track("Flowers", "Sweet Female Attitude", 227, "garage")
        ),
        "bluegrass" to listOf(
            Track("Foggy Mountain Breakdown", "Flatt & Scruggs", 168, "bluegrass"),
            Track("Rain and Snow", "Molly Tuttle", 196, "bluegrass"),
            Track("Blue Night", "Punch Brothers", 271, "bluegrass")
        ),
        "coldwave" to listOf(
            Track("Icy Room", "Xmal Deutschland", 244, "coldwave"),
            Track("Vitrine", "Trisomie 21", 288, "coldwave"),
            Track("Stark Light", "Lebanon Hanover", 232, "coldwave")
        )
    )

    fun forGenre(genreId: String): List<Track> = tracks[genreId] ?: tracks.getValue("jazz")
}
