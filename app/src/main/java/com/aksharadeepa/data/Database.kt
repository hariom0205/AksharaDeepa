package com.aksharadeepa.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val masteryLevel: Int,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = SubjectEntity::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("subjectId")]
)
data class ChapterEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val isCompleted: Boolean,
    val bookContent: String = "", // Summary or book content for reading
    val videoUrl: String = "" // YouTube link for the chapter
)

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(
        entity = ChapterEntity::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId")]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val text: String,
    val options: String, // Stored as comma separated or JSON string
    val correctOptionIndex: Int,
    val explanation: String
)

data class SubjectWithChapters(
    @Embedded val subject: SubjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "subjectId"
    )
    val chapters: List<ChapterEntity>
)

data class ChapterWithQuestions(
    @Embedded val chapter: ChapterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "chapterId"
    )
    val questions: List<QuestionEntity>
)

@Dao
interface AppDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<SubjectWithChapters>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun getChapterWithQuestions(chapterId: String): Flow<ChapterWithQuestions>

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubject(subjectId: String): SubjectEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM flashcards WHERE chapterId = :chapterId")
    fun getFlashcards(chapterId: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFlashcards(flashcards: List<FlashcardEntity>)
}

@Entity(
    tableName = "flashcards",
    foreignKeys = [ForeignKey(
        entity = ChapterEntity::class,
        parentColumns = ["id"],
        childColumns = ["chapterId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("chapterId")]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val term: String,
    val definition: String
)

@Database(entities = [SubjectEntity::class, ChapterEntity::class, QuestionEntity::class, FlashcardEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        internal var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "akshara_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onCreate(db)
        populateDatabase()
    }

    override fun onDestructiveMigration(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        populateDatabase()
    }

    private fun populateDatabase() {
        Executors.newSingleThreadScheduledExecutor().execute {
            AppDatabase.INSTANCE?.appDao()?.let { dao ->
                val subjects = listOf(
                    SubjectEntity("s1", "Science", 0),
                    SubjectEntity("s2", "Math", 0),
                    SubjectEntity("s3", "Social", 0),
                    SubjectEntity("s4", "English", 0),
                    SubjectEntity("s5", "Logical Reasoning", 0)
                )

                val chapters = listOf(
                    // Science
                    ChapterEntity("c1", "s1", "Chemical Reactions", false, "SUMMARY:\nChemical reactions involve breaking and making bonds between atoms to produce new substances. Types include combination, decomposition, displacement, and double displacement reactions. Redox reactions involve simultaneous oxidation and reduction.\n\nINTERESTING FACTS:\n1. The Statue of Liberty turned green due to oxidation.\n2. Your stomach performs decomposition reactions every time you eat.\n3. Fireflies produce light without heat (bioluminescence).\n4. Space rockets move due to a massive combination reaction.", ""),
                    ChapterEntity("c2", "s1", "Acids, Bases and Salts", false, "SUMMARY:\nAcids are sour, turn blue litmus red, and produce H+ ions in water. Bases are bitter, soapy to touch, turn red litmus blue, and produce OH- ions. \n\nKey Concepts:\n1. Indicators: Litmus (Natural), Phenolphthalein (Synthetic), and Onion (Olfactory) help identify acidic/basic media.\n2. Neutralization: Acid + Base -> Salt + Water.\n3. pH Scale (0-14): pH < 7 is acidic, pH = 7 is neutral, pH > 7 is basic.\n4. Important Salts: Baking Soda (NaHCO3) used in baking; Bleaching Powder (CaOCl2) for disinfecting; Plaster of Paris (CaSO4·1/2H2O) for bone setting.", ""),
                    ChapterEntity("c6", "s1", "Metals and Non-metals", false, "SUMMARY:\nMetals are lustrous, malleable, ductile, and good conductors of heat/electricity. Non-metals are generally brittle and poor conductors (except Graphite).\n\nKey Concepts:\n1. Reactivity Series: Arranges metals in decreasing order of reactivity (K > Na > Ca > Mg > Al...).\n2. Ionic Compounds: Formed by transfer of electrons from metal to non-metal. They have high melting points and conduct electricity in molten/aqueous states.\n3. Metallurgy: Processes like Roasting (heating in excess air) and Calcination (limited air) are used to extract metals from ores.\n4. Corrosion Prevention: Methods include painting, oiling, and Galvanization (coating with Zinc).", ""),
                    ChapterEntity("c7", "s1", "Life Processes", false, "SUMMARY:\nBasic functions performed by living organisms to maintain life: nutrition, respiration, transportation, and excretion.\n\nKey Concepts:\n1. Nutrition: Autotrophic (plants make food) and Heterotrophic (animals depend on others).\n2. Respiration: Breaking down glucose to release energy (Aerobic/Anaerobic).\n3. Transportation: Human heart pumps blood through a 'Double Circulation' system.\n4. Excretion: Kidneys filter waste from blood; plants excrete waste via falling leaves or resins.\n\nINTERESTING FACTS:\n1. Lung surface area is ~80 sqm (size of a tennis court).\n2. Your heart pumps ~2,000 gallons of blood daily.\n3. Oxygen is a waste product for plants.", ""),
                    
                    // Math
                    ChapterEntity("c3", "s2", "Real Numbers", false, "SUMMARY:\nReal numbers include Rational (p/q form) and Irrational (non-terminating, non-recurring) numbers.\n\nKey Concepts:\n1. Fundamental Theorem of Arithmetic: Every composite number can be uniquely factorized into primes.\n2. HCF & LCM: HCF uses smallest powers of common prime factors; LCM uses highest powers of all prime factors involved.\n3. Important Rule: HCF(a,b) * LCM(a,b) = a * b.\n4. Rational Decimals: A fraction p/q has a terminating decimal if q is in the form 2^n * 5^m.", ""),
                    ChapterEntity("c4", "s2", "Polynomials", false, "SUMMARY:\nAlgebraic expressions with many terms. The degree (highest power) determines the type: Linear (1), Quadratic (2), Cubic (3).\n\nKey Concepts:\n1. Zeros: The number of times a graph y = p(x) intersects the X-axis is the number of zeros.\n2. Relationship for Quadratic (ax^2 + bx + c):\n   - Sum of zeros (α+β) = -b/a\n   - Product of zeros (αβ) = c/a\n3. Division Algorithm: Dividend = (Divisor * Quotient) + Remainder.\n\nINTERESTING FACTS:\n1. 'Poly' (Greek) means many; 'nomial' (Latin) means term.\n2. Mario's jump path in games is a quadratic polynomial (parabola).", ""),
                    ChapterEntity("c8", "s2", "Quadratic Equations", false, "SUMMARY:\nEquations of the form ax^2 + bx + c = 0 where a ≠ 0.\n\nKey Concepts:\n1. Solution Methods: Factorization (splitting middle term) or the Quadratic Formula: x = [-b ± √(b^2 - 4ac)] / 2a.\n2. Nature of Roots (Discriminant D = b^2 - 4ac):\n   - D > 0: Two distinct real roots.\n   - D = 0: Two equal real roots.\n   - D < 0: No real roots.\n3. Word Problems: Commonly used to solve for ages, area dimensions, or speed/distance scenarios.", ""),
                    ChapterEntity("c9", "s2", "Trigonometry", false, "SUMMARY:\nStudy of relationships between sides and angles of triangles.\n\nKey Concepts:\n1. Ratios: Sin (Opposite/Hypotenuse), Cos (Adjacent/Hypotenuse), Tan (Opposite/Adjacent).\n2. Identities:\n   - sin^2θ + cos^2θ = 1\n   - 1 + tan^2θ = sec^2θ\n   - 1 + cot^2θ = cosec^2θ\n3. Applications: Used in 'Heights and Distances' to find object heights using the Angle of Elevation or Depression.", ""),
                    
                    // Social
                    ChapterEntity("c5", "s3", "Nationalism in Europe", false, "SUMMARY:\nRise of nation-states in 19th-century Europe. Started with the French Revolution (1789) creating a collective identity.\n\nKey Concepts:\n1. Napoleonic Code (1804): Abolished birth privileges, established equality, and secured property rights.\n2. Liberalism: Demanded freedom of markets and government by consent.\n3. Unification: Germany (led by Bismarck) and Italy (led by Cavour, Garibaldi, and Mazzini).\n4. Balkans: Tension in this region eventually led to World War I.\n\nINTERESTING FACTS:\n1. Frédéric Sorrieu's 1848 prints visualized a world of 'social republics'.", ""),
                    ChapterEntity("c10", "s3", "Resources and Development", false, "SUMMARY:\nMaterials in the environment which are technologically accessible, economically feasible, and culturally acceptable are called Resources.\n\nKey Concepts:\n1. Classification: Biotic/Abiotic, Renewable/Non-renewable, Individual/Community/National/International.\n2. Sustainable Development: Meeting needs of today without compromising the future. Agenda 21 (1992 Earth Summit).\n3. Land Resources: 43% Plains, 30% Mountains, 27% Plateaus.\n4. Soil Types in India: Alluvial (fertile), Black (Regur/Cotton soil), Red/Yellow, Laterite, Arid, and Forest soils.\n\nINTERESTING FACTS:\n1. Gandhi said: 'Enough for everyone's need, but not for anyone's greed'.\n2. Soil formation takes millions of years.", ""),
                    ChapterEntity("c11", "s3", "Power Sharing", false, "SUMMARY:\nA strategy to resolve disputes and maintain political stability in a democracy.\n\nKey Concepts:\n1. Belgium Model: Accommodation through equal representation of language groups in government.\n2. Sri Lanka Case: Majoritarianism led to ethnic conflict and civil war.\n3. Reasons for Sharing: Prudential (avoids conflict/instability) and Moral (the spirit of democracy).\n4. Forms: Horizontal (Organs: Leg/Exec/Jud), Vertical (Levels: Centre/State/Local), Social Groups, and Political Parties.", ""),
                    
                    // English
                    ChapterEntity("c12", "s4", "Tenses", false, "SUMMARY:\nTenses indicate the time of action. Divided into Present, Past, and Future.\n\nKey Concepts:\n1. Simple: Facts or habits (e.g., He eats).\n2. Continuous: Action in progress (e.g., He is eating).\n3. Perfect: Finished action (e.g., He has eaten).\n4. Perfect Continuous: Started in past, still continuing (e.g., He has been eating for an hour).\n\nRule Tip: Always match the subject (singular/plural) with the correct helping verb (is/are, has/have).", ""),
                    ChapterEntity("c13", "s4", "Active and Passive Voice", false, "SUMMARY:\nVoice describes the relationship between the action and the participants.\n\nKey Concepts:\n1. Active Voice: The subject performs the action. (e.g., The chef cooked the meal.)\n2. Passive Voice: The subject receives the action. (e.g., The meal was cooked by the chef.)\n3. Core Rules for Passive:\n   - Swap Subject and Object.\n   - Use a form of 'to be' (is/was/been).\n   - Main verb ALWAYS changes to V3 (Past Participle).\n   - Add 'by' before the agent.", ""),
                    
                    // Logical Reasoning
                    ChapterEntity("c14", "s5", "Number Series", false, "SUMMARY:\nIdentifying logical patterns in a sequence of numbers.\n\nKey Concepts:\n1. Arithmetic: Constant difference (e.g., 2, 4, 6...).\n2. Geometric: Constant ratio (e.g., 2, 4, 8...).\n3. Squares/Cubes: Based on n^2 or n^3.\n4. Prime Series: Sequence of prime numbers (2, 3, 5, 7, 11...).\n5. Fibonacci: Each number is the sum of the previous two (1, 1, 2, 3, 5, 8...).", ""),
                    ChapterEntity("c15", "s5", "Blood Relations", false, "SUMMARY:\nSolving relationship puzzles based on family trees.\n\nKey Concepts:\n1. Generations: Separate people by vertical levels in a tree.\n2. Terminology: Paternal (father's side), Maternal (mother's side), Siblings (brothers/sisters), In-laws (by marriage).\n3. Tip: Always start from the person mentioned (e.g., 'His mother...') to trace the path back to the speaker.", ""),
                    ChapterEntity("c16", "s5", "Coding-Decoding", false, "SUMMARY:\nTransforming words/numbers based on specific rules.\n\nKey Concepts:\n1. Letter Shifting: Moving letters forward or backward in the alphabet (e.g., A -> B is +1).\n2. Reverse Coding: Using the opposite end of the alphabet (A -> Z, B -> Y).\n3. Numerical Substitution: Assigning numbers to letters (A=1, B=2).\n4. Deciphering: Finding the common pattern in the example to apply it to the target word.", "")
                )

                val questions = listOf(
                    QuestionEntity("q1", "c1", "What happens when magnesium ribbon is burnt in air?", "It melts,It burns with a dazzling white flame,It turns blue,No reaction", 1, "Magnesium reacts with oxygen to form Magnesium Oxide, which is a white powder."),
                    QuestionEntity("q2", "c1", "Which of the following is a decomposition reaction?", "C+O2->CO2,2H2O->2H2+O2,NaOH+HCl->NaCl+H2O,None", 1, "Water decomposes into Hydrogen and Oxygen when electricity is passed through it."),
                    QuestionEntity("q14", "c1", "Burning of coal is a _______ reaction.", "Combination,Decomposition,Displacement,Double displacement", 0, "C + O2 -> CO2. Two reactants combine to form a single product."),
                    QuestionEntity("q15", "c1", "The process of respiration is ______.", "Exothermic,Endothermic,Reduction,None", 0, "Respiration releases energy, hence it is an exothermic process."),
                    QuestionEntity("q3", "c3", "What is the HCF of 96 and 404 using prime factorization?", "2,4,8,12", 1, "96 = 2^5 * 3, 404 = 2^2 * 101. Common factor is 2^2 = 4."),
                    QuestionEntity("q10", "c14", "Complete the series: 2, 6, 12, 20, 30, ?", "36,40,42,48", 2, "The pattern is n^2 + n or 1*2, 2*3, 3*4, 4*5, 5*6, 6*7. So 6*7 = 42."),
                    QuestionEntity("q11", "c14", "Find the next number: 1, 1, 2, 3, 5, 8, ?", "10,12,13,15", 2, "This is the Fibonacci series where each number is the sum of the previous two. 5+8=13."),
                    QuestionEntity("q12", "c15", "Pointing to a man, a woman says 'His mother is the only daughter of my mother'. How is the woman related to the man?", "Sister,Grandmother,Mother,Aunt", 2, "The 'only daughter of my mother' is the woman herself. So, the man's mother is the woman herself."),
                    QuestionEntity("q13", "c16", "If 'LIGHT' is coded as 'MJHIT', how is 'FLAME' coded?", "GMBNF,GMBND,GLAMF,GNBNF", 0, "Each letter is shifted by +1 in the alphabet. F->G, L->M, A->B, M->N, E->F."),
                    QuestionEntity("q8", "c12", "Which sentence is in the Present Perfect Tense?", "I eat an apple,I am eating an apple,I have eaten an apple,I will eat an apple", 2, "Present Perfect uses 'have/has' + past participle of the verb."),
                    QuestionEntity("q4", "c5", "In which year was the Treaty of Vienna signed?", "1815,1848,1789,1914", 0, "The Treaty of Vienna was signed in 1815 after the defeat of Napoleon.")
                )

                val flashcards = listOf(
                    FlashcardEntity("f1", "c1", "Oxidation", "Gain of oxygen or loss of hydrogen."),
                    FlashcardEntity("f2", "c1", "Reduction", "Loss of oxygen or gain of hydrogen."),
                    FlashcardEntity("f3", "c1", "Exothermic", "Heat is released during reaction."),
                    FlashcardEntity("f4", "c1", "Endothermic", "Heat is absorbed during reaction."),
                    FlashcardEntity("f5", "c3", "Prime Number", "Divisible only by 1 and itself."),
                    FlashcardEntity("f6", "c3", "HCF", "Highest Common Factor of two or more numbers.")
                )

                dao.insertSubjects(subjects)
                dao.insertChapters(chapters)
                dao.insertQuestions(questions)
                dao.insertFlashcards(flashcards)
            }
        }
    }
}
