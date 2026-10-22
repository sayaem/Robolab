package com.example.robolab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robolab.experiments.ExperimentCatalog
import com.example.robolab.experiments.RoboticsExperiment
import com.example.robolab.ui.components.EngineeringHeader
import com.example.robolab.viewmodel.ActiveScreen
import com.example.robolab.viewmodel.RoboLabViewModel
import com.example.ui.theme.*

@Composable
fun ExperimentsScreen(
    viewModel: RoboLabViewModel,
    onNavigate: (ActiveScreen) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("experiments_screen")
    ) {
        EngineeringHeader(
            title = "Robotics Labs",
            subtitle = "Guided Experiments & Engineering Verification",
            badgeText = "${ExperimentCatalog.experiments.size} LABS"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("PRACTICAL ROBOTICS CURRICULUM", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Each lab configures a verified starter setup, programming challenge, and real-time kinematic test condition. Launch an experiment to test physical and sensor behaviors in the simulator.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(ExperimentCatalog.experiments) { exp ->
                ExperimentDetailCard(
                    experiment = exp,
                    onLaunch = {
                        viewModel.launchExperiment(exp)
                        onNavigate(ActiveScreen.SIMULATOR)
                    }
                )
            }
        }
    }
}

@Composable
fun ExperimentDetailCard(
    experiment: RoboticsExperiment,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("experiment_card_${experiment.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DarkOutline)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = experiment.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = CyberAmber.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = experiment.difficulty.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberAmber,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Objective: ${experiment.objective}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = DarkOutline, modifier = Modifier.padding(vertical = 10.dp))

            Text(
                text = "ENGINEERING PRINCIPLE:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TechBlue
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = experiment.engineeringTheory,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFC9D1D9)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "SUCCESS CRITERIA:",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SensorGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = experiment.successCondition,
                fontSize = 11.sp,
                color = Color.White
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DarkOnPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("btn_launch_exp_${experiment.id}")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("LAUNCH EXPERIMENT & OPEN SIMULATOR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}
