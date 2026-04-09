package com.example.dailyexpensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subcategories")
data class SubCategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String = "",
    val name: String = "",
    val iconName: String = "Category",
    val colorHex: String = "#6200EE"
)
