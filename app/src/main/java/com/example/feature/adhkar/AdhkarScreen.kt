package com.example.feature.adhkar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.MusabbihDatabase
import com.example.core.database.entity.DhikrCategoryEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.engine.CounterEngine
import com.example.core.ui.ArabicUtils
import com.example.ui.theme.NotoNaskhArabicFamily
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdhkarScreen(
    database: MusabbihDatabase,
    counterEngine: CounterEngine,
    journeyEngine: com.example.core.engine.JourneyEngine,
    useArabicIndic: Boolean,
    onDhikrSelected: () -> Unit,
    onStartJourney: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dhikrDao = database.dhikrDao()
    val categoryDao = database.categoryDao()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val categories by categoryDao.getAllCategories().collectAsState(initial = emptyList())
    val allDhikrs by (if (searchQuery.isNotBlank()) {
        dhikrDao.searchDhikr(ArabicUtils.normalizeArabic(searchQuery))
    } else {
        dhikrDao.getAllDhikr()
    }).collectAsState(initial = emptyList())

    val filteredDhikrs = allDhikrs.filter { d ->
        val matchesCategory = selectedCategoryId == null || d.categoryId == selectedCategoryId
        val matchesFav = !showOnlyFavorites || d.isFavorite
        matchesCategory && matchesFav
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingDhikr by remember { mutableStateOf<DhikrEntity?>(null) }
    var dhikrToDelete by remember { mutableStateOf<DhikrEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingDhikr = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_custom_dhikr_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة ذكر مخصص")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "مكتبة الأذكار",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                FilterChip(
                    selected = showOnlyFavorites,
                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = { Text("المفضلة") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("filter_favorites_chip")
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .testTag("dhikr_search_field"),
                placeholder = { Text("ابحث في الأذكار بدون تشكيل...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Category Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                        },
                        label = { Text(cat.nameArabic) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Guided Dhikr Journey Banner
            val activeCat = categories.find { it.id == selectedCategoryId }
            val journeyCatId = activeCat?.id ?: "cat_morning"
            val journeyTitle = activeCat?.nameArabic ?: "أذكار الصباح"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "رحلة $journeyTitle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "إتمام متتابع ومرشد لأذكار هذه المجموعة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                journeyEngine.openJourneyOverview(journeyCatId, journeyTitle)
                                onStartJourney()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("start_journey_cta_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "ابدأ", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ابدأ الرحلة")
                    }
                }
            }

            // Dhikr List or Empty State
            if (filteredDhikrs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أذكار مطابقة لبحثك.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredDhikrs, key = { it.id }) { dhikr ->
                        DhikrItemCard(
                            dhikr = dhikr,
                            useArabicIndic = useArabicIndic,
                            onSelect = {
                                scope.launch {
                                    counterEngine.selectDhikr(dhikr.id)
                                    onDhikrSelected()
                                }
                            },
                            onToggleFavorite = {
                                scope.launch {
                                    dhikrDao.setFavorite(dhikr.id, !dhikr.isFavorite)
                                }
                            },
                            onEdit = {
                                editingDhikr = dhikr
                                showAddEditDialog = true
                            },
                            onDelete = {
                                dhikrToDelete = dhikr
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Custom Dhikr Dialog
    if (showAddEditDialog) {
        AddEditDhikrDialog(
            dhikr = editingDhikr,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { text, title, categoryId, target ->
                scope.launch {
                    if (editingDhikr == null) {
                        val newId = UUID.randomUUID().toString()
                        dhikrDao.insertDhikr(
                            DhikrEntity(
                                id = newId,
                                arabicText = text,
                                normalizedSearchText = ArabicUtils.normalizeArabic("$text $title"),
                                title = title.ifBlank { null },
                                categoryId = categoryId,
                                sourceType = "CUSTOM",
                                sourceReference = "ذكر مخصص",
                                verificationStatus = "REVIEWED",
                                defaultTarget = target,
                                isBuiltIn = false,
                                isFavorite = false,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        dhikrDao.updateDhikr(
                            editingDhikr!!.copy(
                                arabicText = text,
                                normalizedSearchText = ArabicUtils.normalizeArabic("$text $title"),
                                title = title.ifBlank { null },
                                categoryId = categoryId,
                                defaultTarget = target,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    showAddEditDialog = false
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (dhikrToDelete != null) {
        AlertDialog(
            onDismissRequest = { dhikrToDelete = null },
            title = { Text("حذف الذكر المخصص") },
            text = { Text("هل أنت متأكد من حذف هذا الذكر المخصص نهائياً؟") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            dhikrDao.deleteCustomDhikr(dhikrToDelete!!.id)
                            dhikrToDelete = null
                        }
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { dhikrToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DhikrItemCard(
    dhikr: DhikrEntity,
    useArabicIndic: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dhikr.title ?: "ذكر",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (!dhikr.isBuiltIn) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "مخصص",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (dhikr.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "تفضيل",
                            tint = if (dhikr.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!dhikr.isBuiltIn) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arabic Text
            Text(
                text = dhikr.arabicText,
                fontFamily = NotoNaskhArabicFamily,
                fontSize = 18.sp,
                lineHeight = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Source and Start Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dhikr.sourceReference,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dhikr.defaultTarget != null) {
                        Text(
                            text = "المستهدف: ${ArabicUtils.formatNumber(dhikr.defaultTarget, useArabicIndic)} مرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = onSelect,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "بدء التسبيح",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ابدأ", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDhikrDialog(
    dhikr: DhikrEntity?,
    categories: List<DhikrCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (text: String, title: String, categoryId: String, target: Long?) -> Unit
) {
    var arabicText by remember { mutableStateOf(dhikr?.arabicText ?: "") }
    var title by remember { mutableStateOf(dhikr?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(dhikr?.categoryId ?: categories.firstOrNull()?.id ?: "cat_general") }
    var targetText by remember { mutableStateOf(dhikr?.defaultTarget?.toString() ?: "") }
    var textError by remember { mutableStateOf(false) }

    var expandedCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (dhikr == null) "إضافة ذكر مخصص" else "تعديل الذكر",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = arabicText,
                    onValueChange = {
                        arabicText = it
                        textError = it.isBlank()
                    },
                    label = { Text("نص الذكر بالعربية *") },
                    isError = textError,
                    modifier = Modifier.fillMaxWidth().testTag("custom_dhikr_text_input"),
                    minLines = 2,
                    maxLines = 4
                )
                if (textError) {
                    Text(
                        text = "نص الذكر مطلوب",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان أو تسمية للذكر (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Selection
                ExposedDropdownMenuBox(
                    expanded = expandedCategoryDropdown,
                    onExpandedChange = { expandedCategoryDropdown = !expandedCategoryDropdown }
                ) {
                    val currentCatName = categories.find { it.id == selectedCategoryId }?.nameArabic ?: "عام"
                    OutlinedTextField(
                        value = currentCatName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategoryDropdown) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategoryDropdown,
                        onDismissRequest = { expandedCategoryDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nameArabic) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expandedCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("الهدف المقترح (اختياري، مثلاً 33)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (arabicText.isBlank()) {
                        textError = true
                    } else {
                        onSave(
                            arabicText.trim(),
                            title.trim(),
                            selectedCategoryId,
                            targetText.toLongOrNull()
                        )
                    }
                },
                modifier = Modifier.testTag("save_custom_dhikr_button")
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
