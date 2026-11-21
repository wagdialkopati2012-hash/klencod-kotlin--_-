package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.domain.model.CompanyInfo
import com.example.ui.theme.LocalTenderingColors
import com.example.ui.viewmodel.AuthViewModel
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LoginScreen(
    needsInitialOwnerSetup: Boolean,
    companyInfo: CompanyInfo,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ext = LocalTenderingColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "loginBg")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ext.heroStart,
                        ext.secondary,
                        ext.heroStart.copy(alpha = 0.94f)
                    )
                )
            )
    ) {
        // Lightweight animated ambient liquid-glass orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = ext.accent.copy(alpha = 0.24f),
                radius = w * 0.48f,
                center = Offset(
                    x = w * 0.2f + cos(phase) * 35f,
                    y = h * 0.18f + sin(phase) * 28f
                )
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = w * 0.42f,
                center = Offset(
                    x = w * 0.85f + sin(phase) * 32f,
                    y = h * 0.78f + cos(phase) * 24f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Company Logo / App Emblem
            Surface(
                modifier = Modifier.size(92.dp),
                shape = RoundedCornerShape(26.dp),
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f)),
                shadowElevation = 10.dp
            ) {
                val logoFile = companyInfo.logoPath?.let { File(it) }?.takeIf { it.exists() }
                if (logoFile != null) {
                    Image(
                        painter = rememberAsyncImagePainter(logoFile),
                        contentDescription = "شعار الشركة",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(26.dp))
                    )
                } else {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
                            Image(
                                painter = painterResource(R.drawable.ic_quote_app_icon),
                                contentDescription = "شعار عرض سعر",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = companyInfo.name.ifBlank { "عرض سعر" },
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (needsInitialOwnerSetup) {
                    "مرحباً بك — قم بإنشاء حساب مدير النظام (المالك) للبدء"
                } else {
                    "مرحباً بك في تطبيق عرض سعر"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Frosted Glass Form Card
            Surface(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            ) {
                AnimatedContent(
                    targetState = needsInitialOwnerSetup,
                    label = "authMode"
                ) { isFirstLaunch ->
                    if (isFirstLaunch) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    text = "تهيئة حساب المالك الأول",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            GlassTextField(
                                value = state.ownerFullName,
                                onValueChange = viewModel::onOwnerFullNameChange,
                                label = "الاسم الكامل للمالك",
                                leadingIcon = Icons.Default.Badge,
                                testTag = "owner_fullname_input"
                            )
                            GlassTextField(
                                value = state.ownerUsername,
                                onValueChange = viewModel::onOwnerUsernameChange,
                                label = "اسم المستخدم",
                                leadingIcon = Icons.Default.Person,
                                testTag = "owner_username_input"
                            )
                            GlassTextField(
                                value = state.ownerPassword,
                                onValueChange = viewModel::onOwnerPasswordChange,
                                label = "كلمة المرور",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                showPassword = state.showPassword,
                                onTogglePassword = viewModel::onToggleShowPassword,
                                testTag = "owner_password_input"
                            )
                            GlassTextField(
                                value = state.ownerConfirmPassword,
                                onValueChange = viewModel::onOwnerConfirmPasswordChange,
                                label = "تأكيد كلمة المرور",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                showPassword = state.showPassword,
                                onTogglePassword = viewModel::onToggleShowPassword,
                                imeAction = ImeAction.Done,
                                onImeAction = viewModel::createInitialOwner,
                                testTag = "owner_confirm_password_input"
                            )

                            AnimatedVisibility(visible = state.errorMessage != null) {
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    color = Color(0xFFFECACA),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = viewModel::createInitialOwner,
                                enabled = !state.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("create_owner_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "إنشاء حساب المدير والبدء",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            GlassTextField(
                                value = state.username,
                                onValueChange = viewModel::onUsernameChange,
                                label = "اسم المستخدم",
                                leadingIcon = Icons.Default.Person,
                                testTag = "login_username_input"
                            )
                            GlassTextField(
                                value = state.password,
                                onValueChange = viewModel::onPasswordChange,
                                label = "كلمة المرور",
                                leadingIcon = Icons.Default.Lock,
                                isPassword = true,
                                showPassword = state.showPassword,
                                onTogglePassword = viewModel::onToggleShowPassword,
                                imeAction = ImeAction.Done,
                                onImeAction = viewModel::login,
                                testTag = "login_password_input"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.rememberUsername,
                                    onCheckedChange = viewModel::onRememberToggle,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ext.accent,
                                        uncheckedColor = Color.White.copy(alpha = 0.7f),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("remember_username_checkbox")
                                )
                                Text(
                                    text = "تذكر اسم المستخدم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }

                            AnimatedVisibility(visible = state.errorMessage != null) {
                                Surface(
                                    color = Color(0xFF991B1B).copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = state.errorMessage.orEmpty(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = viewModel::login,
                                enabled = !state.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ext.accent,
                                    contentColor = Color.White
                                )
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "دخول",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "يعمل 100% بدون إنترنت • الإصدار 1.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.70f)
            )
        }
    }
}

@Composable
private fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.85f)) },
        leadingIcon = {
            Icon(imageVector = leadingIcon, contentDescription = null, tint = Color.White)
        },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "إخفاء كلمة المرور" else "إظهار كلمة المرور",
                        tint = Color.White
                    )
                }
            }
        } else null,
        singleLine = true,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onImeAction?.invoke() }),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.45f),
            cursorColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
