package com.mittimitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mittimitra.ui.theme.MittiMitraTheme
import com.mittimitra.R // <-- ADDED THIS IMPORT

class HelpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MittiMitraTheme {
                HelpScreen(
                    onBackPressed = {
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBackPressed: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help and account") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FaqSection()
            Spacer(modifier = Modifier.height(32.dp))
            ContactSupportSection()
        }
    }
}

@Composable
fun FaqSection() {
    Column {
        Text(
            text = "FAQ",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Re-usable composable for an expandable FAQ item
        FaqItem(
            question = "How accurate is the soil analysis?",
            answer = "Our AI model provides a first-level approximation based on visual data. For precise nutrient levels, we recommend a follow-up lab test. Our app aims to provide immediate, actionable advice."
        )
        Spacer(modifier = Modifier.height(8.dp))
        FaqItem(
            question = "What is Integrated Nutrient Management (INM)?",
            answer = "INM is a strategy that combines organic, inorganic, and biological sources of nutrients to optimize crop production while minimizing environmental impact and maintaining soil health."
        )
    }
}

@Composable
fun FaqItem(question: String, answer: String) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = answer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun ContactSupportSection() {
    Column {
        Text(
            text = "Contact support",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Phone Contact
        ContactRow(
            // Use the icon you added in Step 2
            icon = ImageVector.vectorResource(id = R.drawable.ic_phone),
            iconDescription = "Phone",
            text = "+91 12345 67890" // Placeholder number
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Email Contact
        ContactRow(
            // Use the icon you added in Step 2
            icon = ImageVector.vectorResource(id = R.drawable.ic_email),
            iconDescription = "Email",
            text = "support@mittimitra.com" // Placeholder email
        )
    }
}

@Composable
fun ContactRow(icon: ImageVector, iconDescription: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {

            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HelpScreenPreview() {
    MittiMitraTheme {
        HelpScreen(onBackPressed = {})
    }
}