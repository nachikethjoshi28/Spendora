package com.example.dailyexpensetracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubCategory(subCategory: SubCategoryEntity)

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun getSubCategoriesByCategory(categoryId: String): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM categories WHERE UPPER(name) = UPPER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM subcategories WHERE categoryId = :categoryId AND UPPER(name) = UPPER(:name) LIMIT 1")
    suspend fun getSubCategoryByName(categoryId: String, name: String): SubCategoryEntity?

    @Query("SELECT * FROM subcategories")
    fun getAllSubCategories(): Flow<List<SubCategoryEntity>>
}
