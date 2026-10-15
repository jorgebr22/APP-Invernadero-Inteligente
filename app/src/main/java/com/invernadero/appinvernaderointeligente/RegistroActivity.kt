package com.invernadero.appinvernaderointeligente

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import java.util.Locale

class RegistroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etNombre: TextInputEditText
    private lateinit var etApellidoPaterno: TextInputEditText
    private lateinit var etApellidoMaterno: TextInputEditText
    private lateinit var etCorreoRegistro: TextInputEditText
    private lateinit var etContrasenaRegistro: TextInputEditText
    private lateinit var etConfirmarContrasena: TextInputEditText

    private lateinit var btnCrearCuenta: Button
    private lateinit var tvVolverLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_registro)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        auth = FirebaseAuth.getInstance()

        etNombre = findViewById(R.id.etNombre)
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno)
        etCorreoRegistro = findViewById(R.id.etCorreoRegistro)
        etContrasenaRegistro = findViewById(R.id.etContrasenaRegistro)
        etConfirmarContrasena = findViewById(R.id.etConfirmarContrasena)

        btnCrearCuenta = findViewById(R.id.btnCrearCuenta)
        tvVolverLogin = findViewById(R.id.tvVolverLogin)

        // =========================================================
        // CORREGIR NOMBRE Y APELLIDOS AUTOMÁTICAMENTE
        // =========================================================

        etNombre.setOnFocusChangeListener { _, tieneFoco ->
            if (!tieneFoco) {
                etNombre.setText(
                    capitalizarTexto(etNombre.text.toString())
                )
            }
        }

        etApellidoPaterno.setOnFocusChangeListener { _, tieneFoco ->
            if (!tieneFoco) {
                etApellidoPaterno.setText(
                    capitalizarTexto(etApellidoPaterno.text.toString())
                )
            }
        }

        etApellidoMaterno.setOnFocusChangeListener { _, tieneFoco ->
            if (!tieneFoco) {
                etApellidoMaterno.setText(
                    capitalizarTexto(etApellidoMaterno.text.toString())
                )
            }
        }

        // =========================================================
        // CREAR CUENTA
        // =========================================================
        btnCrearCuenta.setOnClickListener {

            val nombre =
                capitalizarTexto(etNombre.text.toString())

            val apellidoPaterno =
                capitalizarTexto(etApellidoPaterno.text.toString())

            val apellidoMaterno =
                capitalizarTexto(etApellidoMaterno.text.toString())

            val correo =
                etCorreoRegistro.text.toString().trim()

            val contrasena =
                etContrasenaRegistro.text.toString()

            val confirmarContrasena =
                etConfirmarContrasena.text.toString()

            // Mostrar también la corrección en pantalla
            etNombre.setText(nombre)
            etApellidoPaterno.setText(apellidoPaterno)
            etApellidoMaterno.setText(apellidoMaterno)

            // Limpiar errores anteriores
            etNombre.error = null
            etApellidoPaterno.error = null
            etApellidoMaterno.error = null
            etCorreoRegistro.error = null
            etContrasenaRegistro.error = null
            etConfirmarContrasena.error = null

            // =====================================================
            // NOMBRE
            // =====================================================
            if (nombre.isEmpty()) {

                etNombre.error =
                    "Ingrese su nombre"

                etNombre.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // APELLIDO PATERNO
            // =====================================================
            if (apellidoPaterno.isEmpty()) {

                etApellidoPaterno.error =
                    "Ingrese su apellido paterno"

                etApellidoPaterno.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // APELLIDO MATERNO
            // =====================================================
            if (apellidoMaterno.isEmpty()) {

                etApellidoMaterno.error =
                    "Ingrese su apellido materno"

                etApellidoMaterno.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // CORREO
            // =====================================================
            if (correo.isEmpty()) {

                etCorreoRegistro.error =
                    "Ingrese su correo electrónico"

                etCorreoRegistro.requestFocus()

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

                etCorreoRegistro.error =
                    "Ingrese un correo electrónico válido"

                etCorreoRegistro.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // CONTRASEÑA
            // =====================================================
            if (contrasena.isEmpty()) {

                etContrasenaRegistro.error =
                    "Ingrese una contraseña"

                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            if (contrasena.length < 8) {

                val faltan =
                    8 - contrasena.length

                val mensaje =
                    if (faltan == 1) {
                        "Contraseña no permitida. Falta 1 carácter"
                    } else {
                        "Contraseña no permitida. Faltan $faltan caracteres"
                    }

                etContrasenaRegistro.error = mensaje
                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            if (!contrasena.any { it.isUpperCase() }) {

                etContrasenaRegistro.error =
                    "Debe incluir al menos una letra mayúscula"

                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            if (!contrasena.any { it.isLowerCase() }) {

                etContrasenaRegistro.error =
                    "Debe incluir al menos una letra minúscula"

                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            if (!contrasena.any { it.isDigit() }) {

                etContrasenaRegistro.error =
                    "Debe incluir al menos un número"

                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            if (!contrasena.any { !it.isLetterOrDigit() }) {

                etContrasenaRegistro.error =
                    "Debe incluir al menos un símbolo"

                etContrasenaRegistro.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // CONFIRMAR CONTRASEÑA
            // =====================================================
            if (confirmarContrasena.isEmpty()) {

                etConfirmarContrasena.error =
                    "Confirme su contraseña"

                etConfirmarContrasena.requestFocus()

                return@setOnClickListener
            }

            if (contrasena != confirmarContrasena) {

                etConfirmarContrasena.error =
                    "Las contraseñas no coinciden"

                etConfirmarContrasena.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // CREAR CUENTA FIREBASE
            // =====================================================
            btnCrearCuenta.isEnabled = false
            btnCrearCuenta.text = "CREANDO CUENTA..."

            auth.createUserWithEmailAndPassword(
                correo,
                contrasena
            ).addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {

                    val usuario =
                        auth.currentUser

                    if (usuario != null) {

                        // Nombre completo
                        val nombreCompleto =
                            "$nombre $apellidoPaterno $apellidoMaterno"

                        val perfil =
                            userProfileChangeRequest {
                                displayName = nombreCompleto
                            }

                        usuario.updateProfile(perfil)
                            .addOnCompleteListener {

                                // Enviar correo de verificación
                                usuario.sendEmailVerification()
                                    .addOnCompleteListener { verificacionTask ->

                                        btnCrearCuenta.isEnabled = true
                                        btnCrearCuenta.text = "CREAR CUENTA"

                                        if (verificacionTask.isSuccessful) {

                                            Toast.makeText(
                                                this,
                                                "Cuenta creada. Revisa tu correo para verificarla antes de iniciar sesión.",
                                                Toast.LENGTH_LONG
                                            ).show()

                                        } else {

                                            Toast.makeText(
                                                this,
                                                "La cuenta fue creada, pero no se pudo enviar el correo de verificación.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                        auth.signOut()
                                        finish()
                                    }
                            }

                    } else {

                        btnCrearCuenta.isEnabled = true
                        btnCrearCuenta.text = "CREAR CUENTA"

                        Toast.makeText(
                            this,
                            "No se pudo obtener el usuario creado",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } else {

                    btnCrearCuenta.isEnabled = true
                    btnCrearCuenta.text = "CREAR CUENTA"

                    val mensaje =
                        when (task.exception) {

                            is FirebaseAuthUserCollisionException ->
                                "Este correo ya está registrado"

                            is FirebaseAuthWeakPasswordException ->
                                "La contraseña no cumple los requisitos"

                            is FirebaseNetworkException ->
                                "Error de conexión. Revise su Internet"

                            else ->
                                "No se pudo crear la cuenta"
                        }

                    Toast.makeText(
                        this,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        tvVolverLogin.setOnClickListener {
            finish()
        }
    }

    // =============================================================
    // FORMATEAR NOMBRES Y APELLIDOS
    // =============================================================
    private fun capitalizarTexto(texto: String): String {

        val locale =
            Locale("es", "CL")

        return texto
            .trim()
            .lowercase(locale)
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ") { palabra ->

                palabra.replaceFirstChar { caracter ->

                    if (caracter.isLowerCase()) {
                        caracter.titlecase(locale)
                    } else {
                        caracter.toString()
                    }
                }
            }
    }
}