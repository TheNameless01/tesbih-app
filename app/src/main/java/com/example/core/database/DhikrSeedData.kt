package com.example.core.database

import com.example.core.database.entity.DhikrCategoryEntity
import com.example.core.database.entity.DhikrEntity
import com.example.core.ui.ArabicUtils

object DhikrSeedData {

    val categories = listOf(
        DhikrCategoryEntity("cat_tasbeeh", "التسبيح", "أذكار تنزيه الله جل جلاله", 1),
        DhikrCategoryEntity("cat_tahmeed", "التحميد", "أذكار الحمد والثناء والشكر", 2),
        DhikrCategoryEntity("cat_takbeer", "التكبير والتهليل", "أذكار التعظيم والتوحيد", 3),
        DhikrCategoryEntity("cat_istighfar", "الاستغفار", "أدعية طلب المغفرة والتوبة", 4),
        DhikrCategoryEntity("cat_salawat", "الصلاة على النبي", "الصلاة والسلام على رسول الله ﷺ", 5),
        DhikrCategoryEntity("cat_morning", "أذكار الصباح", "أذكار أول النهار وحفظه", 6),
        DhikrCategoryEntity("cat_evening", "أذكار المساء", "أذكار المساء وحفظ الليل", 7),
        DhikrCategoryEntity("cat_after_prayer", "أذكار بعد الصلاة", "الأذكار المشروعة عقب الصلوات المكتوبة", 8),
        DhikrCategoryEntity("cat_quranic_dua", "الأدعية القرآنية", "جوامع الدعاء من كتاب الله تعالى", 9),
        DhikrCategoryEntity("cat_prophetic_dua", "الأدعية النبوية", "الأدعية الثابتة عن النبي ﷺ", 10),
        DhikrCategoryEntity("cat_sleep", "أذكار النوم", "ما يقال عند إرادة النوم", 11),
        DhikrCategoryEntity("cat_waking", "أذكار الاستيقاظ", "ما يقال عند الاستيقاظ من النوم", 12),
        DhikrCategoryEntity("cat_general", "أذكار عامة", "الأذكار اليومية المتنوعة", 13)
    )

    fun getBuiltInDhikrs(): List<DhikrEntity> {
        val list = mutableListOf<DhikrEntity>()

        fun addDhikr(
            id: String,
            text: String,
            title: String,
            categoryId: String,
            sourceType: String,
            sourceRef: String,
            target: Long? = null,
            isFav: Boolean = false
        ) {
            list.add(
                DhikrEntity(
                    id = id,
                    arabicText = text,
                    normalizedSearchText = ArabicUtils.normalizeArabic(text + " " + title),
                    title = title,
                    categoryId = categoryId,
                    sourceType = sourceType,
                    sourceReference = sourceRef,
                    verificationStatus = "REVIEWED",
                    defaultTarget = target,
                    isBuiltIn = true,
                    isFavorite = isFav,
                    createdAt = 1700000000000L,
                    updatedAt = 1700000000000L,
                    contentRevision = 1
                )
            )
        }

        // التسبيح
        addDhikr(
            "dhikr_tasbeeh_01",
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            "تسبيح بحمد الله",
            "cat_tasbeeh",
            "HADITH",
            "صحيح مسلم ٢٦٩١",
            100,
            true
        )
        addDhikr(
            "dhikr_tasbeeh_02",
            "سُبْحَانَ اللَّهِ الْعَظِيمِ وَبِحَمْدِهِ",
            "التسبيح العظيم",
            "cat_tasbeeh",
            "HADITH",
            "صحيح البخاري ٦٤٠٦",
            100,
            true
        )
        addDhikr(
            "dhikr_tasbeeh_03",
            "سُبْحَانَ اللَّهِ",
            "التسبيح المطلق",
            "cat_tasbeeh",
            "HADITH",
            "صحيح مسلم ٥٩٧",
            33,
            true
        )

        // التحميد
        addDhikr(
            "dhikr_tahmeed_01",
            "الْحَمْدُ لِلَّهِ",
            "الحمد لله",
            "cat_tahmeed",
            "HADITH",
            "صحيح مسلم ٥٩٧",
            33,
            true
        )
        addDhikr(
            "dhikr_tahmeed_02",
            "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
            "الحمد لله رب العالمين",
            "cat_tahmeed",
            "QURAN",
            "سورة الفاتحة: ٢",
            100
        )
        addDhikr(
            "dhikr_tahmeed_03",
            "الْحَمْدُ لِلَّهِ حَمْدًا كَثِيرًا طَيِّبًا مُبَارَكًا فِيهِ",
            "حمد مبارك",
            "cat_tahmeed",
            "HADITH",
            "صحيح البخاري ٧٩٩",
            33
        )

        // التكبير والتهليل
        addDhikr(
            "dhikr_takbeer_01",
            "اللَّهُ أَكْبَرُ",
            "التكبير",
            "cat_takbeer",
            "HADITH",
            "صحيح مسلم ٥٩٧",
            34,
            true
        )
        addDhikr(
            "dhikr_takbeer_02",
            "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "سيد الأذكار",
            "cat_takbeer",
            "HADITH",
            "صحيح البخاري ٣٢٩٣، صحيح مسلم ٢٦٩١",
            100,
            true
        )
        addDhikr(
            "dhikr_takbeer_03",
            "لَا إِلَهَ إِلَّا اللَّهُ",
            "التهليل",
            "cat_takbeer",
            "HADITH",
            "صحيح مسلم ٢٦",
            100
        )

        // الاستغفار
        addDhikr(
            "dhikr_istighfar_01",
            "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            "الاستغفار والتوبة",
            "cat_istighfar",
            "HADITH",
            "صحيح البخاري ٦٣٠٧",
            100,
            true
        )
        addDhikr(
            "dhikr_istighfar_02",
            "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            "استغفار التوبة العظيم",
            "cat_istighfar",
            "HADITH",
            "سنن أبي داود ١٥١٧، الترمذي ٣٥٧٧",
            3
        )
        addDhikr(
            "dhikr_istighfar_03",
            "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "سيد الاستغفار",
            "cat_istighfar",
            "HADITH",
            "صحيح البخاري ٦٣٠٦",
            1,
            true
        )

        // الصلاة على النبي ﷺ
        addDhikr(
            "dhikr_salawat_01",
            "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            "الصلاة على النبي ﷺ",
            "cat_salawat",
            "HADITH",
            "سنن النسائي ١٢٩٧",
            10,
            true
        )
        addDhikr(
            "dhikr_salawat_02",
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ، اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ",
            "الصلاة الإبراهيمية",
            "cat_salawat",
            "HADITH",
            "صحيح البخاري ٣٣٧٠",
            10
        )

        // أذكار الصباح والمساء
        addDhikr(
            "dhikr_morning_01",
            "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "أصبحنا وأصبح الملك لله",
            "cat_morning",
            "HADITH",
            "صحيح مسلم ٢٧٢٣",
            1
        )
        addDhikr(
            "dhikr_evening_01",
            "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "أمسينا وأمسى الملك لله",
            "cat_evening",
            "HADITH",
            "صحيح مسلم ٢٧٢٣",
            1
        )
        addDhikr(
            "dhikr_morning_02",
            "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            "حفظ الصباح والمساء",
            "cat_morning",
            "HADITH",
            "سنن أبي داود ٥٠٨٨، الترمذي ٣٣٨٨",
            3,
            true
        )
        addDhikr(
            "dhikr_morning_03",
            "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
            "الرضا بالله ربا",
            "cat_morning",
            "HADITH",
            "سنن أبي داود ٥٠٧٢",
            3
        )
        addDhikr(
            "dhikr_morning_04",
            "حَسْبِيَ اللَّهُ لَا إِلَهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            "الكفاية والتوكل",
            "cat_morning",
            "QURAN",
            "سورة التوبة: ١٢٩، سنن أبي داود ٥٠٨١",
            7
        )

        // أذكار بعد الصلاة
        addDhikr(
            "dhikr_post_prayer_01",
            "أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            "استغفار دبر كل صلاة",
            "cat_after_prayer",
            "HADITH",
            "صحيح مسلم ٥٩١",
            1
        )

        // الأدعية القرآنية
        addDhikr(
            "dhikr_quran_01",
            "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            "جامع خيري الدنيا والآخرة",
            "cat_quranic_dua",
            "QURAN",
            "سورة البقرة: ٢٠١",
            10
        )
        addDhikr(
            "dhikr_quran_02",
            "لَا إِلَهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنْتُ مِنَ الظَّالِمِينَ",
            "دعوة ذي النون",
            "cat_quranic_dua",
            "QURAN",
            "سورة الأنبياء: ٨٧",
            40,
            true
        )
        addDhikr(
            "dhikr_quran_03",
            "رَبِّ اشْرَحْ لِي صَدْرِي * وَيَسِّرْ لِي أَمْرِي",
            "دعاء انشراح الصدر والتيسير",
            "cat_quranic_dua",
            "QURAN",
            "سورة طه: ٢٥-٢٦",
            10
        )
        addDhikr(
            "dhikr_quran_04",
            "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            "دعاء موسى عليه السلام",
            "cat_quranic_dua",
            "QURAN",
            "سورة القصص: ٢٤",
            10
        )

        // الأدعية النبوية
        addDhikr(
            "dhikr_prophetic_01",
            "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            "تثبيت القلب",
            "cat_prophetic_dua",
            "HADITH",
            "جامع الترمذي ٢١٤٠",
            10,
            true
        )
        addDhikr(
            "dhikr_prophetic_02",
            "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ",
            "سؤال العفو والعافية",
            "cat_prophetic_dua",
            "HADITH",
            "سنن أبي داود ٥٠٧٤",
            3
        )

        // أذكار النوم والاستيقاظ
        addDhikr(
            "dhikr_sleep_01",
            "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي وَبِكَ أَرْفَعُهُ، إِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ",
            "دعاء النوم",
            "cat_sleep",
            "HADITH",
            "صحيح البخاري ٦٣٢٠",
            1
        )
        addDhikr(
            "dhikr_wake_01",
            "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "دعاء الاستيقاظ",
            "cat_waking",
            "HADITH",
            "صحيح البخاري ٦٣١٢",
            1
        )

        // أذكار عامة
        addDhikr(
            "dhikr_general_01",
            "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "كنز من كنوز الجنة",
            "cat_general",
            "HADITH",
            "صحيح البخاري ٦٣٨٤، مسلم ٢٧٠٤",
            100,
            true
        )

        return list
    }
}
