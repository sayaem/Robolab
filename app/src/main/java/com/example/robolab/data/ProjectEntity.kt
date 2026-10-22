package com.example.robolab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "robot_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val formatVersion: Int = 1,
    val assemblyJson: String,
    val programJson: String,
    val obstaclesJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
