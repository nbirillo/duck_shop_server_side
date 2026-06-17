package org.jetbrains.kotlin.course.duck.shop.shop

import org.jetbrains.kotlin.course.duck.shop.duck.Accessory
import org.jetbrains.kotlin.course.duck.shop.duck.Duck
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/** Seeds the duck & accessory catalog once, if the database is empty. */
@Component
class CatalogSeeder(
    private val duckRepository: DuckRepository,
    private val accessoryRepository: AccessoryRepository,
) : CommandLineRunner {
    override fun run(vararg args: String) {
        if (duckRepository.count() > 0) return

        val hat = accessoryRepository.save(Accessory("Hat", 75))
        val monocle = accessoryRepository.save(Accessory("Monocle", 90))
        val flag = accessoryRepository.save(Accessory("Flag", 30))
        val crown = accessoryRepository.save(Accessory("Crown", 100))
        val eyePatch = accessoryRepository.save(Accessory("PirateEyePatch", 15))
        val tie = accessoryRepository.save(Accessory("Tie", 25))
        val glasses = accessoryRepository.save(Accessory("Glasses", 110))
        val medal = accessoryRepository.save(Accessory("Medal", 18))
        val pin = accessoryRepository.save(Accessory("Pin", 6))
        val tShirt = accessoryRepository.save(Accessory("TShirt", 45))

        duckRepository.saveAll(
            listOf(
                Duck("Alex", accessories = setOf(monocle, hat)),
                Duck("Daniel", hasKotlinAttribute = true, accessories = setOf(flag)),
                Duck("Dorian", accessories = setOf(crown)),
                Duck("Jack", accessories = setOf(eyePatch)),
                Duck("Kristian", accessories = setOf(tie)),
                Duck("Leo", accessories = setOf(glasses)),
                Duck("MrPink", displayName = "Mr. Pink", accessories = setOf(medal)),
                Duck("Oliver"),
                Duck("Piter", accessories = setOf(hat, pin)),
                Duck("Vanessa", hasKotlinAttribute = true, accessories = setOf(tShirt)),
            ),
        )
    }
}
