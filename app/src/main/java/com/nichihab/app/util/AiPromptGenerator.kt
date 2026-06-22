package com.nichihab.app.util

/**
 * Local rule-based prompt generator that maps abstract tasks to concrete photo requirements.
 * Before the user snaps a verification photo, the AI gives a specific, contextual prompt.
 */
object AiPromptGenerator {

    data class PhotoPrompt(
        val instruction: String,
        val hint: String,
        val keywords: List<String>
    )

    private data class TaskRule(
        val triggers: List<String>,
        val prompts: Map<String, PhotoPrompt>
    )

    private val rules = listOf(
        TaskRule(
            triggers = listOf("shower", "ducha", "douche", "banho", "dusche", "bath", "baño"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of the running water or your towel", "Show evidence you're ready to shower", listOf("water", "towel", "bathroom", "shower", "faucet")),
                "es" to PhotoPrompt("Toma una foto del agua corriendo o tu toalla", "Muestra evidencia de que estás listo para ducharte", listOf("agua", "toalla", "baño", "ducha")),
                "fr" to PhotoPrompt("Prenez une photo de l'eau qui coule ou de votre serviette", "Montrez que vous êtes prêt à vous doucher", listOf("eau", "serviette", "douche", "salle de bain")),
                "pt" to PhotoPrompt("Tire uma foto da água correndo ou da sua toalha", "Mostre evidência de que está pronto para o banho", listOf("água", "toalha", "banheiro", "chuveiro")),
                "de" to PhotoPrompt("Machen Sie ein Foto vom laufenden Wasser oder Ihrem Handtuch", "Zeigen Sie, dass Sie bereit zum Duschen sind", listOf("Wasser", "Handtuch", "Badezimmer", "Dusche"))
            )
        ),
        TaskRule(
            triggers = listOf("study", "estudiar", "étudier", "estudar", "lernen", "homework", "tarea", "devoir"),
            prompts = mapOf(
                "en" to PhotoPrompt("Snap a photo of your open textbook or notes", "Show your study materials ready on your desk", listOf("book", "textbook", "notes", "notebook", "desk", "laptop", "screen")),
                "es" to PhotoPrompt("Toma una foto de tu libro abierto o apuntes", "Muestra tus materiales de estudio listos", listOf("libro", "apuntes", "cuaderno", "escritorio")),
                "fr" to PhotoPrompt("Prenez une photo de votre livre ouvert ou vos notes", "Montrez vos matériaux d'étude prêts", listOf("livre", "notes", "cahier", "bureau")),
                "pt" to PhotoPrompt("Tire uma foto do seu livro aberto ou anotações", "Mostre seus materiais de estudo prontos", listOf("livro", "anotações", "caderno", "mesa")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres offenen Lehrbuchs oder Ihrer Notizen", "Zeigen Sie Ihre Lernmaterialien bereit", listOf("Buch", "Notizen", "Heft", "Schreibtisch"))
            )
        ),
        TaskRule(
            triggers = listOf("gym", "exercise", "workout", "ejercicio", "exercice", "exercício", "training", "sport"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of the gym equipment or your workout space", "Show where you're about to exercise", listOf("gym", "weights", "treadmill", "mat", "exercise", "equipment", "sneakers")),
                "es" to PhotoPrompt("Toma una foto del equipo de gimnasio o tu espacio de ejercicio", "Muestra dónde vas a ejercitarte", listOf("gimnasio", "pesas", "colchoneta", "equipo")),
                "fr" to PhotoPrompt("Prenez une photo de l'équipement de gym ou votre espace d'entraînement", "Montrez où vous allez faire de l'exercice", listOf("gym", "poids", "tapis", "équipement")),
                "pt" to PhotoPrompt("Tire uma foto do equipamento ou do seu espaço de treino", "Mostre onde você vai se exercitar", listOf("academia", "pesos", "tapete", "equipamento")),
                "de" to PhotoPrompt("Machen Sie ein Foto der Fitnessgeräte oder Ihres Trainingsbereichs", "Zeigen Sie, wo Sie trainieren werden", listOf("Fitnessstudio", "Gewichte", "Matte", "Geräte"))
            )
        ),
        TaskRule(
            triggers = listOf("read", "leer", "lire", "ler", "lesen", "book", "libro", "livre", "livro", "buch"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your open book or e-reader", "Show what you're reading", listOf("book", "pages", "kindle", "e-reader", "reading")),
                "es" to PhotoPrompt("Toma una foto de tu libro abierto o e-reader", "Muestra lo que estás leyendo", listOf("libro", "páginas", "kindle", "lectura")),
                "fr" to PhotoPrompt("Prenez une photo de votre livre ouvert ou liseuse", "Montrez ce que vous lisez", listOf("livre", "pages", "kindle", "liseuse")),
                "pt" to PhotoPrompt("Tire uma foto do seu livro aberto ou e-reader", "Mostre o que você está lendo", listOf("livro", "páginas", "kindle", "leitura")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres offenen Buches oder E-Readers", "Zeigen Sie, was Sie lesen", listOf("Buch", "Seiten", "Kindle", "E-Reader"))
            )
        ),
        TaskRule(
            triggers = listOf("walk", "caminar", "marcher", "caminhar", "spazieren", "run", "correr", "courir", "laufen", "jog"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of the path or scenery outside", "Show you're outdoors and moving", listOf("path", "road", "sidewalk", "park", "outside", "trees", "shoes")),
                "es" to PhotoPrompt("Toma una foto del camino o paisaje exterior", "Muestra que estás afuera y en movimiento", listOf("camino", "carretera", "parque", "exterior")),
                "fr" to PhotoPrompt("Prenez une photo du chemin ou du paysage dehors", "Montrez que vous êtes dehors et en mouvement", listOf("chemin", "route", "parc", "extérieur")),
                "pt" to PhotoPrompt("Tire uma foto do caminho ou paisagem lá fora", "Mostre que você está ao ar livre", listOf("caminho", "estrada", "parque", "exterior")),
                "de" to PhotoPrompt("Machen Sie ein Foto des Weges oder der Landschaft draußen", "Zeigen Sie, dass Sie draußen und in Bewegung sind", listOf("Weg", "Straße", "Park", "draußen"))
            )
        ),
        TaskRule(
            triggers = listOf("bed", "cama", "lit", "bett", "make bed", "hacer la cama", "faire le lit"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your neatly made bed", "Show pillows and blanket arranged", listOf("bed", "pillow", "blanket", "sheets", "bedroom")),
                "es" to PhotoPrompt("Toma una foto de tu cama tendida", "Muestra las almohadas y cobijas arregladas", listOf("cama", "almohada", "cobija", "sábanas")),
                "fr" to PhotoPrompt("Prenez une photo de votre lit bien fait", "Montrez les oreillers et la couverture bien rangés", listOf("lit", "oreiller", "couverture", "draps")),
                "pt" to PhotoPrompt("Tire uma foto da sua cama arrumada", "Mostre travesseiros e cobertor organizados", listOf("cama", "travesseiro", "cobertor", "lençóis")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres gemachten Bettes", "Zeigen Sie ordentlich arrangierte Kissen und Decke", listOf("Bett", "Kissen", "Decke", "Bettwäsche"))
            )
        ),
        TaskRule(
            triggers = listOf("water", "drink", "agua", "beber", "eau", "boire", "wasser", "trinken"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your glass or bottle of water", "Show you're staying hydrated", listOf("glass", "water", "bottle", "cup")),
                "es" to PhotoPrompt("Toma una foto de tu vaso o botella de agua", "Muestra que te mantienes hidratado", listOf("vaso", "agua", "botella")),
                "fr" to PhotoPrompt("Prenez une photo de votre verre ou bouteille d'eau", "Montrez que vous restez hydraté", listOf("verre", "eau", "bouteille")),
                "pt" to PhotoPrompt("Tire uma foto do seu copo ou garrafa de água", "Mostre que você está se hidratando", listOf("copo", "água", "garrafa")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres Glases oder Ihrer Wasserflasche", "Zeigen Sie, dass Sie hydriert bleiben", listOf("Glas", "Wasser", "Flasche"))
            )
        ),
        TaskRule(
            triggers = listOf("meditat", "yoga", "stretch", "estirar", "étirer", "alongar", "dehnen"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your meditation or yoga space", "Show your mat, cushion, or calm spot", listOf("mat", "yoga", "cushion", "meditation", "floor", "candle")),
                "es" to PhotoPrompt("Toma una foto de tu espacio de meditación o yoga", "Muestra tu colchoneta o espacio tranquilo", listOf("colchoneta", "yoga", "meditación", "cojín")),
                "fr" to PhotoPrompt("Prenez une photo de votre espace de méditation ou yoga", "Montrez votre tapis ou espace calme", listOf("tapis", "yoga", "méditation", "coussin")),
                "pt" to PhotoPrompt("Tire uma foto do seu espaço de meditação ou yoga", "Mostre seu tapete ou espaço tranquilo", listOf("tapete", "yoga", "meditação", "almofada")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres Meditations- oder Yogabereichs", "Zeigen Sie Ihre Matte oder Ihren ruhigen Platz", listOf("Matte", "Yoga", "Meditation", "Kissen"))
            )
        ),
        TaskRule(
            triggers = listOf("clean", "tidy", "limpiar", "ordenar", "nettoyer", "ranger", "limpar", "arrumar", "putzen", "aufräumen", "desk", "workspace"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your clean workspace or area", "Show the tidied-up result", listOf("desk", "clean", "organized", "tidy", "workspace", "table")),
                "es" to PhotoPrompt("Toma una foto de tu espacio limpio y ordenado", "Muestra el resultado organizado", listOf("escritorio", "limpio", "ordenado", "mesa")),
                "fr" to PhotoPrompt("Prenez une photo de votre espace propre et rangé", "Montrez le résultat rangé", listOf("bureau", "propre", "rangé", "table")),
                "pt" to PhotoPrompt("Tire uma foto do seu espaço limpo e organizado", "Mostre o resultado arrumado", listOf("mesa", "limpo", "organizado", "escritório")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres sauberen Arbeitsbereichs", "Zeigen Sie das aufgeräumte Ergebnis", listOf("Schreibtisch", "sauber", "ordentlich", "Tisch"))
            )
        ),
        TaskRule(
            triggers = listOf("journal", "write", "escribir", "écrire", "escrever", "schreiben", "diary", "diario"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your journal or written page", "Show what you've written", listOf("journal", "notebook", "pen", "writing", "page", "diary")),
                "es" to PhotoPrompt("Toma una foto de tu diario o página escrita", "Muestra lo que has escrito", listOf("diario", "cuaderno", "pluma", "escritura")),
                "fr" to PhotoPrompt("Prenez une photo de votre journal ou page écrite", "Montrez ce que vous avez écrit", listOf("journal", "cahier", "stylo", "écriture")),
                "pt" to PhotoPrompt("Tire uma foto do seu diário ou página escrita", "Mostre o que você escreveu", listOf("diário", "caderno", "caneta", "escrita")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihres Tagebuchs oder der geschriebenen Seite", "Zeigen Sie, was Sie geschrieben haben", listOf("Tagebuch", "Heft", "Stift", "Schrift"))
            )
        ),
        TaskRule(
            triggers = listOf("cook", "cocinar", "cuisiner", "cozinhar", "kochen", "breakfast", "desayuno", "petit-déjeuner", "café da manhã", "frühstück", "meal", "comida"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of the food you prepared", "Show your meal or ingredients", listOf("food", "plate", "kitchen", "cooking", "meal", "breakfast")),
                "es" to PhotoPrompt("Toma una foto de la comida que preparaste", "Muestra tu plato o ingredientes", listOf("comida", "plato", "cocina", "ingredientes")),
                "fr" to PhotoPrompt("Prenez une photo de la nourriture préparée", "Montrez votre plat ou ingrédients", listOf("nourriture", "assiette", "cuisine", "ingrédients")),
                "pt" to PhotoPrompt("Tire uma foto da comida que preparou", "Mostre seu prato ou ingredientes", listOf("comida", "prato", "cozinha", "ingredientes")),
                "de" to PhotoPrompt("Machen Sie ein Foto des zubereiteten Essens", "Zeigen Sie Ihre Mahlzeit oder Zutaten", listOf("Essen", "Teller", "Küche", "Zutaten"))
            )
        ),
        TaskRule(
            triggers = listOf("push-up", "pushup", "flexion", "pompe", "flexão", "liegestütz", "sit-up", "plank", "squat"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo of your exercise position or your workout area", "Show you're ready to exercise", listOf("floor", "mat", "exercise", "position", "workout")),
                "es" to PhotoPrompt("Toma una foto de tu posición de ejercicio o área de entrenamiento", "Muestra que estás listo para ejercitarte", listOf("suelo", "colchoneta", "ejercicio", "posición")),
                "fr" to PhotoPrompt("Prenez une photo de votre position d'exercice ou espace d'entraînement", "Montrez que vous êtes prêt", listOf("sol", "tapis", "exercice", "position")),
                "pt" to PhotoPrompt("Tire uma foto da sua posição de exercício ou área de treino", "Mostre que está pronto para se exercitar", listOf("chão", "tapete", "exercício", "posição")),
                "de" to PhotoPrompt("Machen Sie ein Foto Ihrer Übungsposition oder Ihres Trainingsbereichs", "Zeigen Sie, dass Sie bereit sind", listOf("Boden", "Matte", "Übung", "Position"))
            )
        ),
        TaskRule(
            triggers = listOf("phone", "teléfono", "téléphone", "telefone", "telefon", "another room", "otra habitación"),
            prompts = mapOf(
                "en" to PhotoPrompt("Take a photo showing your phone placed away from your desk", "Show your phone in another room or out of reach", listOf("phone", "desk", "room", "away", "table")),
                "es" to PhotoPrompt("Toma una foto mostrando tu teléfono lejos de tu escritorio", "Muestra tu teléfono en otra habitación", listOf("teléfono", "escritorio", "habitación")),
                "fr" to PhotoPrompt("Prenez une photo montrant votre téléphone loin de votre bureau", "Montrez votre téléphone dans une autre pièce", listOf("téléphone", "bureau", "pièce")),
                "pt" to PhotoPrompt("Tire uma foto mostrando seu telefone longe da mesa", "Mostre seu telefone em outro cômodo", listOf("telefone", "mesa", "cômodo")),
                "de" to PhotoPrompt("Machen Sie ein Foto, das Ihr Telefon entfernt vom Schreibtisch zeigt", "Zeigen Sie Ihr Telefon in einem anderen Raum", listOf("Telefon", "Schreibtisch", "Raum"))
            )
        )
    )

    private val fallbackPrompts = mapOf(
        "en" to PhotoPrompt("Take a photo showing evidence of completing this task", "Show clear proof of your progress", listOf("evidence", "completed", "done", "progress")),
        "es" to PhotoPrompt("Toma una foto mostrando evidencia de completar esta tarea", "Muestra prueba clara de tu progreso", listOf("evidencia", "completado", "progreso")),
        "fr" to PhotoPrompt("Prenez une photo montrant la preuve de l'accomplissement", "Montrez une preuve claire de votre progrès", listOf("preuve", "accompli", "progrès")),
        "pt" to PhotoPrompt("Tire uma foto mostrando evidência de completar esta tarefa", "Mostre prova clara do seu progresso", listOf("evidência", "completado", "progresso")),
        "de" to PhotoPrompt("Machen Sie ein Foto, das den Abschluss dieser Aufgabe zeigt", "Zeigen Sie einen klaren Beweis Ihres Fortschritts", listOf("Beweis", "erledigt", "Fortschritt"))
    )

    fun getPhotoPrompt(taskTitle: String, language: String = "en"): PhotoPrompt {
        val lang = if (language in listOf("en", "es", "fr", "pt", "de")) language else "en"
        val titleLower = taskTitle.lowercase()

        for (rule in rules) {
            if (rule.triggers.any { titleLower.contains(it) }) {
                return rule.prompts[lang] ?: rule.prompts["en"]!!
            }
        }
        return fallbackPrompts[lang] ?: fallbackPrompts["en"]!!
    }
}
