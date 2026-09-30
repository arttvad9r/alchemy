package com.artt.alchemy.game

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** The strings of one language's `elements.xml`, read straight from the resource file. */
class ElementResources(directory: String) {
    private val strings: Map<String, String> = run {
        val file = File("src/main/res/$directory/elements.xml")
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getElementsByTagName("string")
        (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    fun name(id: String): String? = strings["element_name_$id"]

    fun fact(id: String): String? = strings["element_fact_$id"]

    companion object {
        val russian = ElementResources("values")
        val english = ElementResources("values-en")
    }
}
