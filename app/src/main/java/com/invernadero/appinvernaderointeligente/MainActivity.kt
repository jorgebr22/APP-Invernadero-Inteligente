package com.invernadero.appinvernaderointeligente

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etUsuario: TextInputEditText
    private lateinit var etContrasena: TextInputEditText

    private lateinit var btnIngresar: Button
    private lateinit var tvCrearCuenta: TextView
    private lateinit var tvOlvideContrasena: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

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

        etUsuario = findViewById(R.id.etUsuario)
        etContrasena = findViewById(R.id.etContrasena)

        btnIngresar = findViewById(R.id.btnIngresar)
        tvCrearCuenta = findViewById(R.id.tvCrearCuenta)
        tvOlvideContrasena = findViewById(R.id.tvOlvideContrasena)

        // =========================================================
        // INICIAR SESIÓN
        // =========================================================
        btnIngresar.setOnClickListener {

            val correo = etUsuario.text.toString().trim()
            val contrasena = etContrasena.text.toString()

            etUsuario.error = null
            etContrasena.error = null

            if (correo.isEmpty()) {

                etUsuario.error =
                    "Ingrese su correo electrónico"

                etUsuario.requestFocus()

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

                etUsuario.error =
                    "Ingrese un correo electrónico válido"

                etUsuario.requestFocus()

                return@setOnClickListener
            }

            if (contrasena.isEmpty()) {

                etContrasena.error =
                    "Ingrese su contraseña"

                etContrasena.requestFocus()

                return@setOnClickListener
            }

            btnIngresar.isEnabled = false
            btnIngresar.text = "INGRESANDO..."

            auth.signInWithEmailAndPassword(
                correo,
                contrasena
            ).addOnCompleteListener(this) { task ->

                btnIngresar.isEnabled = true
                btnIngresar.text = "INGRESAR"

                if (task.isSuccessful) {

                    val usuario = auth.currentUser

                    if (usuario != null) {

                        // =========================================
                        // COMPROBAR VERIFICACIÓN DEL CORREO
                        // =========================================
                        if (usuario.isEmailVerified) {

                            Toast.makeText(
                                this,
                                "Inicio de sesión correcto",
                                Toast.LENGTH_SHORT
                            ).show()

                            val intent =
                                Intent(
                                    this,
                                    MonitoreoActivity::class.java
                                )

                            startActivity(intent)
                            finish()

                        } else {

                            Toast.makeText(
                                this,
                                "Debes verificar tu correo electrónico antes de ingresar.",
                                Toast.LENGTH_LONG
                            ).show()

                            // No permitir acceso
                            auth.signOut()
                        }
                    }

                } else {

                    val mensaje =
                        if (task.exception is FirebaseNetworkException) {

                            "Error de conexión. Revise su Internet"

                        } else {

                            "Correo o contraseña incorrectos. Intenta nuevamente."
                        }

                    Toast.makeText(
                        this,
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()

                    etContrasena.text?.clear()
                    etContrasena.requestFocus()
                }
            }
        }

        // =========================================================
        // CREAR CUENTA
        // =========================================================
        tvCrearCuenta.setOnClickListener {

            val intent =
                Intent(
                    this,
                    RegistroActivity::class.java
                )

            startActivity(intent)
        }

        // =========================================================
        // ¿OLVIDASTE TU CONTRASEÑA?
        // =========================================================
        tvOlvideContrasena.setOnClickListener {

            val correo =
                etUsuario.text.toString().trim()

            etUsuario.error = null

            if (correo.isEmpty()) {

                etUsuario.error =
                    "Escriba su correo para recuperar la contraseña"

                etUsuario.requestFocus()

                Toast.makeText(
                    this,
                    "Ingrese primero su correo electrónico",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {

                etUsuario.error =
                    "Ingrese un correo electrónico válido"

                etUsuario.requestFocus()

                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(correo)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Correo de recuperación enviado. Revise su bandeja de entrada.",
                            Toast.LENGTH_LONG
                        ).show()

                    } else {

                        val mensaje =
                            if (task.exception is FirebaseNetworkException) {

                                "Error de conexión. Revise su Internet"

                            } else {

                                "No se pudo enviar el correo de recuperación"
                            }

                        Toast.makeText(
                            this,
                            mensaje,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}