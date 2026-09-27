package com.artt.alchemy.game

object AlchemyCatalog {
    val baseElementIds: Set<String> = setOf("fire", "water", "earth", "air")

    val elements: List<ElementDefinition> = listOf(
        element("fire", "Огонь", ElementGroup.NATURE),
        element("water", "Вода", ElementGroup.NATURE),
        element("earth", "Земля", ElementGroup.NATURE),
        element("air", "Воздух", ElementGroup.NATURE),
        element("steam", "Пар", ElementGroup.NATURE),
        element("lava", "Лава", ElementGroup.NATURE),
        element("energy", "Энергия", ElementGroup.NATURE),
        element("mud", "Грязь", ElementGroup.NATURE),
        element("rain", "Дождь", ElementGroup.NATURE),
        element("dust", "Пыль", ElementGroup.NATURE),
        element("sea", "Море", ElementGroup.NATURE),
        element("stone", "Камень", ElementGroup.NATURE),
        element("cloud", "Облако", ElementGroup.NATURE),
        element("lightning", "Молния", ElementGroup.NATURE),
        element("sand", "Песок", ElementGroup.NATURE),
        element("clay", "Глина", ElementGroup.NATURE),
        element("ash", "Пепел", ElementGroup.NATURE),
        element("smoke", "Дым", ElementGroup.NATURE),
        element("wind", "Ветер", ElementGroup.NATURE),
        element("ice", "Лёд", ElementGroup.NATURE),
        element("snow", "Снег", ElementGroup.NATURE),
        element("frost", "Иней", ElementGroup.NATURE),
        element("storm", "Буря", ElementGroup.NATURE),
        element("volcano", "Вулкан", ElementGroup.NATURE),
        element("mountain", "Гора", ElementGroup.NATURE),
        element("river", "Река", ElementGroup.NATURE),
        element("lake", "Озеро", ElementGroup.NATURE),
        element("forest", "Лес", ElementGroup.NATURE),
        element("swamp", "Болото", ElementGroup.NATURE),
        element("desert", "Пустыня", ElementGroup.NATURE),
        element("island", "Остров", ElementGroup.NATURE),
        element("ocean", "Океан", ElementGroup.NATURE),
        element("wave", "Волна", ElementGroup.NATURE),
        element("mist", "Туман", ElementGroup.NATURE),
        element("metal", "Металл", ElementGroup.NATURE),
        element("glass", "Стекло", ElementGroup.NATURE),
        element("crystal", "Кристалл", ElementGroup.NATURE),
        element("salt", "Соль", ElementGroup.NATURE),
        element("oil", "Нефть", ElementGroup.NATURE),
        element("coal", "Уголь", ElementGroup.NATURE),
        element("brick", "Кирпич", ElementGroup.MATERIAL),
        element("concrete", "Бетон", ElementGroup.MATERIAL),
        element("ceramic", "Керамика", ElementGroup.MATERIAL),
        element("paper", "Бумага", ElementGroup.MATERIAL),
        element("ink", "Чернила", ElementGroup.MATERIAL),
        element("wood", "Древесина", ElementGroup.MATERIAL),
        element("fabric", "Ткань", ElementGroup.MATERIAL),
        element("leather", "Кожа", ElementGroup.MATERIAL),
        element("rope", "Верёвка", ElementGroup.MATERIAL),
        element("tool", "Инструмент", ElementGroup.MATERIAL),
        element("wheel", "Колесо", ElementGroup.MATERIAL),
        element("clock", "Часы", ElementGroup.MATERIAL),
        element("wire", "Провод", ElementGroup.MATERIAL),
        element("battery", "Батарея", ElementGroup.MATERIAL),
        element("engine", "Двигатель", ElementGroup.MATERIAL),
        element("machine", "Машина", ElementGroup.MATERIAL),
        element("plastic", "Пластик", ElementGroup.MATERIAL),
        element("rubber", "Резина", ElementGroup.MATERIAL),
        element("paint", "Краска", ElementGroup.MATERIAL),
        element("glue", "Клей", ElementGroup.MATERIAL),
        element("house", "Дом", ElementGroup.MATERIAL),
        element("road", "Дорога", ElementGroup.MATERIAL),
        element("bridge", "Мост", ElementGroup.MATERIAL),
        element("boat", "Лодка", ElementGroup.MATERIAL),
        element("ship", "Корабль", ElementGroup.MATERIAL),
        element("seed", "Семя", ElementGroup.LIFE),
        element("grass", "Трава", ElementGroup.LIFE),
        element("flower", "Цветок", ElementGroup.LIFE),
        element("tree", "Дерево", ElementGroup.LIFE),
        element("moss", "Мох", ElementGroup.LIFE),
        element("mushroom", "Гриб", ElementGroup.LIFE),
        element("algae", "Водоросли", ElementGroup.LIFE),
        element("bacteria", "Бактерии", ElementGroup.LIFE),
        element("life", "Жизнь", ElementGroup.LIFE),
        element("fish", "Рыба", ElementGroup.LIFE),
        element("bird", "Птица", ElementGroup.LIFE),
        element("beast", "Зверь", ElementGroup.LIFE),
        element("human", "Человек", ElementGroup.LIFE),
        element("livestock", "Скот", ElementGroup.LIFE),
        element("insect", "Насекомое", ElementGroup.LIFE),
        element("worm", "Червь", ElementGroup.LIFE),
        element("egg", "Яйцо", ElementGroup.LIFE),
        element("bone", "Кость", ElementGroup.LIFE),
        element("blood", "Кровь", ElementGroup.LIFE),
        element("medicine", "Лекарство", ElementGroup.LIFE),
        element("food", "Еда", ElementGroup.LIFE),
        element("wheat", "Пшеница", ElementGroup.LIFE),
        element("fruit", "Фрукт", ElementGroup.LIFE),
        element("vegetable", "Овощ", ElementGroup.LIFE),
        element("honey", "Мёд", ElementGroup.LIFE),
        element("village", "Деревня", ElementGroup.CIVILIZATION),
        element("city", "Город", ElementGroup.CIVILIZATION),
        element("country", "Страна", ElementGroup.CIVILIZATION),
        element("castle", "Замок", ElementGroup.CIVILIZATION),
        element("library", "Библиотека", ElementGroup.CIVILIZATION),
        element("school", "Школа", ElementGroup.CIVILIZATION),
        element("book", "Книга", ElementGroup.CIVILIZATION),
        element("letter", "Письмо", ElementGroup.CIVILIZATION),
        element("map", "Карта", ElementGroup.CIVILIZATION),
        element("radio", "Радио", ElementGroup.CIVILIZATION),
        element("telephone", "Телефон", ElementGroup.CIVILIZATION),
        element("camera", "Камера", ElementGroup.CIVILIZATION),
        element("computer", "Компьютер", ElementGroup.CIVILIZATION),
        element("robot", "Робот", ElementGroup.CIVILIZATION),
        element("music", "Музыка", ElementGroup.CIVILIZATION),
        element("art", "Искусство", ElementGroup.CIVILIZATION),
        element("science", "Наука", ElementGroup.CIVILIZATION),
        element("law", "Закон", ElementGroup.CIVILIZATION),
        element("market", "Рынок", ElementGroup.CIVILIZATION),
        element("factory", "Фабрика", ElementGroup.CIVILIZATION),
        element("sun", "Солнце", ElementGroup.COSMOS),
        element("moon", "Луна", ElementGroup.COSMOS),
        element("star", "Звезда", ElementGroup.COSMOS),
        element("sky", "Небо", ElementGroup.COSMOS),
        element("planet", "Планета", ElementGroup.COSMOS),
        element("space", "Космос", ElementGroup.COSMOS),
        element("galaxy", "Галактика", ElementGroup.COSMOS),
        element("time", "Время", ElementGroup.COSMOS),
        element("light", "Свет", ElementGroup.COSMOS),
        element("shadow", "Тень", ElementGroup.COSMOS)
    )

    val elementsById: Map<String, ElementDefinition> =
        elements.associateBy(ElementDefinition::id).also { definitions ->
            check(definitions.size == elements.size) { "Element IDs must be unique" }
            check(baseElementIds.all(definitions::containsKey)) { "Base elements must exist" }
        }

    val recipes: List<Recipe> = buildRecipes()

    val recipeResultsByKey: Map<String, String> =
        recipes.associate { recipe -> recipeKey(recipe.firstId, recipe.secondId) to recipe.resultId }.also { results ->
            check(recipes.size == 180) { "Catalog must contain exactly 180 recipes" }
            check(results.size == recipes.size) { "Recipe ingredient pairs must be unique" }
            check(
                recipes.all { recipe ->
                    recipe.firstId in elementsById &&
                        recipe.secondId in elementsById &&
                        recipe.resultId in elementsById
                }
            ) { "Recipes may only reference catalog elements" }
        }

    private fun element(id: String, name: String, group: ElementGroup): ElementDefinition = ElementDefinition(id = id, name = name, group = group, color = group.color)

    private fun buildRecipes(): List<Recipe> {
        val derivedIds = elements.map(ElementDefinition::id).filterNot(baseElementIds::contains)
        val unlockRecipes = listOf(
            Recipe("fire", "water", "steam"),
            Recipe("fire", "earth", "lava"),
            Recipe("fire", "air", "energy"),
            Recipe("water", "earth", "mud"),
            Recipe("water", "air", "rain"),
            Recipe("earth", "air", "dust")
        ) + derivedIds.drop(6).mapIndexed { index, resultId ->
            Recipe(
                firstId = baseElementIds.sorted()[index % baseElementIds.size],
                secondId = derivedIds[index + 5],
                resultId = resultId
            )
        }

        val usedKeys = unlockRecipes.map { recipeKey(it.firstId, it.secondId) }.toMutableSet()
        val bonusRecipes = buildList {
            for (firstIndex in elements.indices) {
                for (secondIndex in firstIndex + 1 until elements.size) {
                    if (size == 64) return@buildList
                    val firstId = elements[firstIndex].id
                    val secondId = elements[secondIndex].id
                    if (usedKeys.add(recipeKey(firstId, secondId))) {
                        add(Recipe(firstId, secondId, derivedIds[size % derivedIds.size]))
                    }
                }
            }
        }

        return unlockRecipes + bonusRecipes
    }
}
