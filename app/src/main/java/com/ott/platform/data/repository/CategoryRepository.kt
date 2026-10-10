package com.ott.platform.data.repository

import com.ott.platform.data.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActiveCategories(tenantId: String): Flow<List<Category>>
    fun observeAllCategories(tenantId: String): Flow<List<Category>>
    suspend fun getCategoryById(categoryId: String): Category?
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(categoryId: String) // Often just sets active=false
}
