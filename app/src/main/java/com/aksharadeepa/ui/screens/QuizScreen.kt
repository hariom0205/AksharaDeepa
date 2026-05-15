package com.aksharadeepa.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aksharadeepa.data.ChapterWithQuestions
import com.aksharadeepa.data.QuestionEntity
import com.aksharadeepa.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(subjectId: String, chapterId: String, viewModel: MainViewModel, navController: NavController) {
    var chapterData by remember { mutableStateOf<ChapterWithQuestions?>(null) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val selectedOptions = remember { mutableStateMapOf<Int, Int>() }
    var timeLeft by remember { mutableIntStateOf(300) } // 5 mins
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(chapterId) {
        chapterData = viewModel.getChapterWithQuestions(chapterId)
    }

    LaunchedEffect(isFinished, chapterData) {
        if (isFinished || (chapterData == null)) return@LaunchedEffect
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
            if (timeLeft == 0) {
                finishQuiz(chapterData!!, selectedOptions, viewModel, subjectId, chapterId) {
                    isFinished = true
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFinished) "Results" else "Knowledge Test") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isFinished) {
                        TimerDisplay(timeLeft)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (chapterData == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (chapterData!!.questions.isEmpty()) {
                Text("No questions available for this chapter.", modifier = Modifier.align(Alignment.Center))
            } else if (isFinished) {
                ReviewScreen(
                    chapterData = chapterData!!,
                    selectedOptions = selectedOptions,
                    onBack = { navController.popBackStack() }
                )
            } else {
                QuizContent(
                    questions = chapterData!!.questions,
                    currentIndex = currentQuestionIndex,
                    selectedOptions = selectedOptions,
                    onNext = { if (currentQuestionIndex < chapterData!!.questions.size - 1) currentQuestionIndex++ },
                    onPrev = { if (currentQuestionIndex > 0) currentQuestionIndex-- },
                    onSubmit = {
                        finishQuiz(chapterData!!, selectedOptions, viewModel, subjectId, chapterId) {
                            isFinished = true
                        }
                    }
                )
            }
        }
    }
}

private fun finishQuiz(
    chapterData: ChapterWithQuestions,
    selectedOptions: Map<Int, Int>,
    viewModel: MainViewModel,
    subjectId: String,
    chapterId: String,
    onComplete: () -> Unit
) {
    val score = calculateScore(chapterData, selectedOptions)
    viewModel.submitQuiz(subjectId, chapterId, score)
    onComplete()
}

@Composable
fun TimerDisplay(timeLeft: Int) {
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    
    Surface(
        color = if (timeLeft < 60) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (timeLeft < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                timeString,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun QuizContent(
    questions: List<QuestionEntity>,
    currentIndex: Int,
    selectedOptions: Map<Int, Int>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSubmit: () -> Unit
) {
    val question = questions[currentIndex]
    val options = question.options.split(",")
    val progress = (currentIndex + 1).toFloat() / questions.size
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Progress Info
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question ${currentIndex + 1} of ${questions.size}", style = MaterialTheme.typography.labelLarge)
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Question Text
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Text(
                question.text,
                modifier = Modifier.padding(24.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options List
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEachIndexed { index, option ->
                OptionItem(
                    text = option,
                    isSelected = selectedOptions[currentIndex] == index,
                    onClick = { (selectedOptions as MutableMap)[currentIndex] = index }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = onPrev,
                enabled = currentIndex > 0,
                modifier = Modifier.height(56.dp).weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Previous")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            if (currentIndex == questions.size - 1) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.height(56.dp).weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Finish Test", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier.height(56.dp).weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Next")
                }
            }
        }
    }
}

@Composable
fun OptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text,
                fontSize = 16.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ReviewScreen(chapterData: ChapterWithQuestions, selectedOptions: Map<Int, Int>, onBack: () -> Unit) {
    val correctCount = chapterData.questions.indices.count { i -> selectedOptions[i] == chapterData.questions[i].correctOptionIndex }
    val scorePercent = (correctCount.toFloat() / chapterData.questions.size * 100).toInt()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (scorePercent >= 60) "Excellent Work! 🌟" else "Keep Practicing! 💪",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CircularScoreIndicator(scorePercent, correctCount, chapterData.questions.size)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Return to Course")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Answer Review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            
            items(chapterData.questions) { q ->
                val index = chapterData.questions.indexOf(q)
                val userAnsIndex = selectedOptions[index]
                val isCorrect = userAnsIndex == q.correctOptionIndex
                val options = q.options.split(",")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCorrect) Color(0xFF10B981).copy(alpha = 0.05f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCorrect) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(q.text, fontWeight = FontWeight.Bold)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (userAnsIndex != null) options[userAnsIndex] else "Not answered",
                                fontSize = 14.sp,
                                color = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }
                        
                        if (!isCorrect) {
                            Text(
                                "Correct: ${options[q.correctOptionIndex]}",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                q.explanation,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CircularScoreIndicator(score: Int, correct: Int, total: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 10.dp,
            color = if (score >= 60) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$correct/$total", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("Score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun calculateScore(chapterData: ChapterWithQuestions, selectedOptions: Map<Int, Int>): Int {
    var correctCount = 0
    chapterData.questions.forEachIndexed { index, q ->
        if (selectedOptions[index] == q.correctOptionIndex) {
            correctCount++
        }
    }
    return if (chapterData.questions.isNotEmpty()) {
        ((correctCount.toFloat() / chapterData.questions.size) * 100).toInt()
    } else 0
}
