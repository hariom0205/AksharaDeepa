package com.aksharadeepa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aksharadeepa.data.AppDatabase
import com.aksharadeepa.data.ChapterWithQuestions
import com.aksharadeepa.data.FlashcardEntity
import com.aksharadeepa.data.SubjectWithChapters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.appDao()

    private val _subjects = MutableStateFlow<List<SubjectWithChapters>>(emptyList())
    val subjects: StateFlow<List<SubjectWithChapters>> = _subjects.asStateFlow()

    init {
        viewModelScope.launch {
            dao.getAllSubjects().collect {
                _subjects.value = it
            }
        }
    }

    suspend fun getChapterWithQuestions(chapterId: String): ChapterWithQuestions {
        return dao.getChapterWithQuestions(chapterId).first()
    }

    suspend fun getFlashcards(chapterId: String): List<FlashcardEntity> {
        return dao.getFlashcards(chapterId).first()
    }

    fun submitQuiz(subjectId: String, chapterId: String, scorePercent: Int) {
        viewModelScope.launch {
            // Update Chapter
            val subjectsList = _subjects.value
            val subjectWithChapters = subjectsList.find { it.subject.id == subjectId }
            val chapter = subjectWithChapters?.chapters?.find { it.id == chapterId }
            
            chapter?.let {
                dao.updateChapter(it.copy(isCompleted = true))
            }

            // Update Subject Mastery
            if (subjectWithChapters != null) {
                val sub = subjectWithChapters.subject
                val newMastery = minOf(100, sub.masteryLevel + (scorePercent / 5)) // Mock logic
                dao.updateSubject(sub.copy(masteryLevel = newMastery))
            }
        }
    }
}
