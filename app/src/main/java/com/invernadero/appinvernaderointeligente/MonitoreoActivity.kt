package com.invernadero.appinvernaderointeligente

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MonitoreoActivity : AppCompatActivity() {

    // =============================================================
    // FIREBASE AUTHENTICATION
    // =============================================================

    private lateinit var auth: FirebaseAuth

    // =============================================================
    // FIREBASE REALTIME DATABASE
    // =============================================================

    private lateinit var database: FirebaseDatabase

    private lateinit var sensoresRef: DatabaseReference
    private lateinit var actuadoresRef: DatabaseReference
    private lateinit var conexionRef: DatabaseReference

    // =============================================================
    // TEXTOS DE LOS SENSORES
    // =============================================================

    private lateinit var tvTemperatura: TextView
    private lateinit var tvHumedadAmbiental: TextView
    private lateinit var tvHumedadSuelo1: TextView
    private lateinit var tvHumedadSuelo2: TextView
    private lateinit var tvLuminosidad: TextView
    private lateinit var tvNivelAgua: TextView

    // =============================================================
    // ESTADO DEL SISTEMA
    // =============================================================

    private lateinit var tvEstadoConexion: TextView

    // =============================================================
    // ACTUADORES
    // =============================================================

    private lateinit var switchBomba: MaterialSwitch
    private lateinit var switchVentilacion: MaterialSwitch

    /*
     * Estas variables evitan que una actualización recibida desde
     * Firebase vuelva a escribir innecesariamente el mismo valor.
     */
    private var actualizandoBombaDesdeFirebase = false
    private var actualizandoVentilacionDesdeFirebase = false

    // =============================================================
    // MENÚ
    // =============================================================

    private lateinit var btnMenu: TextView
    private lateinit var btnCerrarSesion: TextView

    // =============================================================
    // PERFIL
    // =============================================================

    private var imgPerfilDialogActual: ImageView? = null

    // =============================================================
    // LISTENERS DE FIREBASE
    // =============================================================

    private var listenerSensores: ValueEventListener? = null
    private var listenerActuadores: ValueEventListener? = null
    private var listenerConexion: ValueEventListener? = null

    // =============================================================
    // SELECTOR DE FOTO DE PERFIL
    // =============================================================

    private val seleccionarFotoLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {

                val guardadaCorrectamente =
                    guardarFotoPerfilEnAlmacenamientoInterno(uri)

                if (guardadaCorrectamente) {

                    mostrarFotoPerfilGuardada(
                        imgPerfilDialogActual
                    )

                    Toast.makeText(
                        this,
                        "Foto de perfil actualizada",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo guardar la foto de perfil",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    // =============================================================
    // ON CREATE
    // =============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_monitoreo
        )

        // =========================================================
        // AJUSTAR PANTALLA
        // =========================================================

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // =========================================================
        // FIREBASE AUTHENTICATION
        // =========================================================

        auth =
            FirebaseAuth.getInstance()

        val usuarioActual =
            auth.currentUser

        /*
         * Solo permitimos entrar al Panel si existe una sesión
         * y el correo fue verificado.
         */
        if (
            usuarioActual == null ||
            !usuarioActual.isEmailVerified
        ) {

            auth.signOut()

            volverAlLogin()

            return
        }

        // =========================================================
        // FIREBASE REALTIME DATABASE
        // =========================================================

        database =
            FirebaseDatabase.getInstance()

        sensoresRef =
            database.reference
                .child("invernadero")
                .child("sensores")

        actuadoresRef =
            database.reference
                .child("invernadero")
                .child("actuadores")

        /*
         * Firebase incluye esta ruta especial para conocer
         * el estado de conexión con Realtime Database.
         */
        conexionRef =
            database.reference
                .child(".info")
                .child("connected")

        // =========================================================
        // ELEMENTOS DEL PANEL DE MONITOREO
        // =========================================================

        tvTemperatura =
            findViewById(
                R.id.tvTemperatura
            )

        tvHumedadAmbiental =
            findViewById(
                R.id.tvHumedadAmbiental
            )

        tvHumedadSuelo1 =
            findViewById(
                R.id.tvHumedadSuelo1
            )

        tvHumedadSuelo2 =
            findViewById(
                R.id.tvHumedadSuelo2
            )

        tvLuminosidad =
            findViewById(
                R.id.tvLuminosidad
            )

        tvNivelAgua =
            findViewById(
                R.id.tvNivelAgua
            )

        // =========================================================
        // ESTADO DEL SISTEMA
        // =========================================================

        tvEstadoConexion =
            findViewById(
                R.id.tvEstadoConexion
            )

        // =========================================================
        // ACTUADORES
        // =========================================================

        switchBomba =
            findViewById(
                R.id.switchBomba
            )

        switchVentilacion =
            findViewById(
                R.id.switchVentilacion
            )

        // =========================================================
        // MENÚ
        // =========================================================

        btnMenu =
            findViewById(
                R.id.btnMenu
            )

        btnCerrarSesion =
            findViewById(
                R.id.btnCerrarSesion
            )

        // =========================================================
        // ESTADO VISUAL INICIAL
        // =========================================================

        mostrarSistemaDesconectado()

        switchBomba.text =
            getString(
                R.string.estado_off
            )

        switchVentilacion.text =
            getString(
                R.string.estado_off
            )

        // =========================================================
        // INICIAR FIREBASE REALTIME
        // =========================================================

        escucharConexionFirebase()

        escucharSensoresFirebase()

        escucharActuadoresFirebase()

        configurarControlesActuadores()

        // =========================================================
        // MENÚ
        // =========================================================

        btnMenu.setOnClickListener {

            mostrarMenu()
        }

        // =========================================================
        // CERRAR SESIÓN
        // =========================================================

        btnCerrarSesion.setOnClickListener {

            mostrarConfirmacionCerrarSesion()
        }
    }

    // =============================================================
    // CONEXIÓN CON FIREBASE
    // =============================================================

    private fun escucharConexionFirebase() {

        listenerConexion =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val conectado =
                        snapshot.getValue(
                            Boolean::class.java
                        ) ?: false

                    if (conectado) {

                        mostrarSistemaConectado()

                    } else {

                        mostrarSistemaDesconectado()
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    mostrarSistemaDesconectado()
                }
            }

        conexionRef.addValueEventListener(
            listenerConexion!!
        )
    }

    // =============================================================
    // MOSTRAR SISTEMA CONECTADO
    // =============================================================

    private fun mostrarSistemaConectado() {

        tvEstadoConexion.text =
            "● Conectado"

        tvEstadoConexion.setTextColor(
            Color.parseColor(
                "#2E7D32"
            )
        )
    }

    // =============================================================
    // MOSTRAR SISTEMA DESCONECTADO
    // =============================================================

    private fun mostrarSistemaDesconectado() {

        tvEstadoConexion.text =
            "● Desconectado"

        tvEstadoConexion.setTextColor(
            Color.parseColor(
                "#C62828"
            )
        )
    }

    // =============================================================
    // LEER SENSORES DESDE FIREBASE
    // =============================================================

    private fun escucharSensoresFirebase() {

        listenerSensores =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    // =================================================
                    // TEMPERATURA
                    // =================================================

                    val temperatura =
                        snapshot
                            .child("temperatura")
                            .getValue(
                                Double::class.java
                            )

                    if (temperatura != null) {

                        tvTemperatura.text =
                            "${formatearNumero(temperatura)} °C"

                    } else {

                        tvTemperatura.text =
                            "-- °C"
                    }

                    // =================================================
                    // HUMEDAD AMBIENTAL
                    // =================================================

                    val humedadAmbiental =
                        obtenerNumeroFirebase(
                            snapshot,
                            "humedadAmbiental"
                        )

                    if (humedadAmbiental != null) {

                        tvHumedadAmbiental.text =
                            "${formatearNumero(humedadAmbiental)} %"

                    } else {

                        tvHumedadAmbiental.text =
                            "-- %"
                    }

                    // =================================================
                    // HUMEDAD SUELO 1
                    // =================================================

                    val humedadSuelo1 =
                        obtenerNumeroFirebase(
                            snapshot,
                            "humedadSuelo1"
                        )

                    if (humedadSuelo1 != null) {

                        tvHumedadSuelo1.text =
                            "${formatearNumero(humedadSuelo1)} %"

                    } else {

                        tvHumedadSuelo1.text =
                            "-- %"
                    }

                    // =================================================
                    // HUMEDAD SUELO 2
                    // =================================================

                    val humedadSuelo2 =
                        obtenerNumeroFirebase(
                            snapshot,
                            "humedadSuelo2"
                        )

                    if (humedadSuelo2 != null) {

                        tvHumedadSuelo2.text =
                            "${formatearNumero(humedadSuelo2)} %"

                    } else {

                        tvHumedadSuelo2.text =
                            "-- %"
                    }

                    // =================================================
                    // LUMINOSIDAD
                    // =================================================

                    val luminosidad =
                        obtenerNumeroFirebase(
                            snapshot,
                            "luminosidad"
                        )

                    if (luminosidad != null) {

                        tvLuminosidad.text =
                            "${formatearNumero(luminosidad)} %"

                    } else {

                        tvLuminosidad.text =
                            "-- %"
                    }

                    // =================================================
                    // NIVEL DE AGUA
                    // =================================================

                    val nivelAgua =
                        obtenerNumeroFirebase(
                            snapshot,
                            "nivelAgua"
                        )

                    if (nivelAgua != null) {

                        tvNivelAgua.text =
                            "${formatearNumero(nivelAgua)} %"

                    } else {

                        tvNivelAgua.text =
                            "-- %"
                    }
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    Toast.makeText(
                        this@MonitoreoActivity,
                        "No se pudieron leer los sensores",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        sensoresRef.addValueEventListener(
            listenerSensores!!
        )
    }

    // =============================================================
    // OBTENER NÚMERO DESDE FIREBASE
    // =============================================================

    private fun obtenerNumeroFirebase(
        snapshot: DataSnapshot,
        nombreCampo: String
    ): Double? {

        val valor =
            snapshot
                .child(nombreCampo)
                .value

        return when (valor) {

            is Long ->
                valor.toDouble()

            is Double ->
                valor

            is Float ->
                valor.toDouble()

            is Int ->
                valor.toDouble()

            else ->
                null
        }
    }

    // =============================================================
    // FORMATEAR NÚMEROS
    // =============================================================

    private fun formatearNumero(
        valor: Double
    ): String {

        return if (
            valor % 1.0 == 0.0
        ) {

            valor.toInt()
                .toString()

        } else {

            String.format(
                Locale.US,
                "%.1f",
                valor
            )
        }
    }

    // =============================================================
    // ESCUCHAR ACTUADORES DESDE FIREBASE
    // =============================================================

    private fun escucharActuadoresFirebase() {

        listenerActuadores =
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    // =================================================
                    // BOMBA
                    // =================================================

                    val bombaActiva =
                        snapshot
                            .child("bomba")
                            .getValue(
                                Boolean::class.java
                            ) ?: false

                    actualizandoBombaDesdeFirebase =
                        true

                    switchBomba.isChecked =
                        bombaActiva

                    switchBomba.text =
                        if (bombaActiva) {

                            getString(
                                R.string.estado_on
                            )

                        } else {

                            getString(
                                R.string.estado_off
                            )
                        }

                    actualizandoBombaDesdeFirebase =
                        false

                    // =================================================
                    // VENTILACIÓN
                    // =================================================

                    val ventilacionActiva =
                        snapshot
                            .child("ventilacion")
                            .getValue(
                                Boolean::class.java
                            ) ?: false

                    actualizandoVentilacionDesdeFirebase =
                        true

                    switchVentilacion.isChecked =
                        ventilacionActiva

                    switchVentilacion.text =
                        if (ventilacionActiva) {

                            getString(
                                R.string.estado_on
                            )

                        } else {

                            getString(
                                R.string.estado_off
                            )
                        }

                    actualizandoVentilacionDesdeFirebase =
                        false
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {

                    Toast.makeText(
                        this@MonitoreoActivity,
                        "No se pudieron leer los actuadores",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        actuadoresRef.addValueEventListener(
            listenerActuadores!!
        )
    }

    // =============================================================
    // CONFIGURAR CONTROLES DE ACTUADORES
    // =============================================================

    private fun configurarControlesActuadores() {

        // =========================================================
        // BOMBA
        // =========================================================

        switchBomba.setOnCheckedChangeListener { _, isChecked ->

            switchBomba.text =
                if (isChecked) {

                    getString(
                        R.string.estado_on
                    )

                } else {

                    getString(
                        R.string.estado_off
                    )
                }

            /*
             * Si el cambio vino desde Firebase,
             * no volvemos a escribirlo.
             */
            if (
                actualizandoBombaDesdeFirebase
            ) {

                return@setOnCheckedChangeListener
            }

            actuadoresRef
                .child("bomba")
                .setValue(isChecked)
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "No se pudo actualizar la bomba",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // =========================================================
        // VENTILACIÓN
        // =========================================================

        switchVentilacion.setOnCheckedChangeListener { _, isChecked ->

            switchVentilacion.text =
                if (isChecked) {

                    getString(
                        R.string.estado_on
                    )

                } else {

                    getString(
                        R.string.estado_off
                    )
                }

            if (
                actualizandoVentilacionDesdeFirebase
            ) {

                return@setOnCheckedChangeListener
            }

            actuadoresRef
                .child("ventilacion")
                .setValue(isChecked)
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "No se pudo actualizar la ventilación",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    // =============================================================
    // MENÚ PRINCIPAL
    // =============================================================

    private fun mostrarMenu() {

        val popupMenu =
            PopupMenu(
                this,
                btnMenu
            )

        popupMenu.menu.add(
            "Mi cuenta"
        )

        popupMenu.menu.add(
            "Comprobación del sistema"
        )

        popupMenu.menu.add(
            "Reinicio de sistema"
        )

        popupMenu.menu.add(
            "Acerca de GreenHouse"
        )

        popupMenu.setOnMenuItemClickListener { item ->

            when (
                item.title.toString()
            ) {

                "Mi cuenta" -> {

                    mostrarMiCuenta()

                    true
                }

                "Comprobación del sistema" -> {

                    comprobarSistema()

                    true
                }

                "Reinicio de sistema" -> {

                    mostrarConfirmacionReinicio()

                    true
                }

                "Acerca de GreenHouse" -> {

                    mostrarAcercaDe()

                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    // =============================================================
    // FOTO DE PERFIL - ALMACENAMIENTO INTERNO
    // =============================================================

    private fun obtenerArchivoFotoPerfil(): File {

        return File(
            filesDir,
            "foto_perfil.jpg"
        )
    }

    private fun guardarFotoPerfilEnAlmacenamientoInterno(
        uri: Uri
    ): Boolean {

        return try {

            val archivoDestino =
                obtenerArchivoFotoPerfil()

            val entrada =
                contentResolver.openInputStream(uri)
                    ?: return false

            entrada.use { input ->

                FileOutputStream(
                    archivoDestino,
                    false
                ).use { output ->

                    input.copyTo(output)
                }
            }

            true

        } catch (_: Exception) {

            false
        }
    }

    private fun mostrarFotoPerfilGuardada(
        imageView: ImageView?
    ) {

        if (imageView == null) {
            return
        }

        val archivoFoto =
            obtenerArchivoFotoPerfil()

        if (
            archivoFoto.exists() &&
            archivoFoto.length() > 0L
        ) {

            imageView.setPadding(
                0,
                0,
                0,
                0
            )

            imageView.scaleType =
                ImageView.ScaleType.CENTER_CROP

            imageView.setImageDrawable(
                null
            )

            imageView.setImageURI(
                Uri.fromFile(
                    archivoFoto
                )
            )

            imageView.clipToOutline =
                true

        } else {

            imageView.setImageResource(
                R.drawable.ic_person_profile
            )
        }
    }

    // =============================================================
    // MI CUENTA
    // =============================================================

    private fun mostrarMiCuenta() {

        val usuario =
            auth.currentUser

        if (usuario == null) {

            Toast.makeText(
                this,
                "No se pudo obtener la información de la cuenta",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_mi_cuenta,
                null
            )

        // =========================================================
        // ELEMENTOS
        // =========================================================

        val imgPerfil =
            vista.findViewById<ImageView>(
                R.id.imgPerfil
            )

        val btnEditarFoto =
            vista.findViewById<ImageButton>(
                R.id.btnEditarFoto
            )

        val etNombrePerfil =
            vista.findViewById<TextInputEditText>(
                R.id.etNombrePerfil
            )

        val etCorreoPerfil =
            vista.findViewById<TextInputEditText>(
                R.id.etCorreoPerfil
            )

        val tvEstadoCuenta =
            vista.findViewById<TextView>(
                R.id.tvEstadoCorreoPerfil
            )

        val btnCancelarPerfil =
            vista.findViewById<Button>(
                R.id.btnCancelarPerfil
            )

        val btnGuardarPerfil =
            vista.findViewById<Button>(
                R.id.btnGuardarPerfil
            )

        // =========================================================
        // FOTO DE PERFIL
        // =========================================================

        imgPerfilDialogActual =
            imgPerfil

        mostrarFotoPerfilGuardada(
            imgPerfil
        )

        // =========================================================
        // CAMBIAR FOTO
        // =========================================================

        btnEditarFoto.setOnClickListener {

            seleccionarFotoLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts
                        .PickVisualMedia
                        .ImageOnly
                )
            )
        }

        // =========================================================
        // DATOS DE LA CUENTA
        // =========================================================

        etNombrePerfil.setText(
            usuario.displayName ?: ""
        )

        etCorreoPerfil.setText(
            usuario.email ?: ""
        )

        // =========================================================
        // ESTADO DE LA CUENTA
        // =========================================================

        if (
            usuario.isEmailVerified
        ) {

            tvEstadoCuenta.text =
                "✓ Verificado"

            tvEstadoCuenta.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvEstadoCuenta.text =
                "● No verificado"

            tvEstadoCuenta.setTextColor(
                Color.parseColor(
                    "#C62828"
                )
            )
        }

        // =========================================================
        // CREAR DIÁLOGO
        // =========================================================

        val dialogo =
            AlertDialog.Builder(this)
                .setView(vista)
                .create()

        dialogo.setOnShowListener {

            dialogo.window
                ?.setBackgroundDrawableResource(
                    android.R.color.transparent
                )
        }

        dialogo.setOnDismissListener {

            imgPerfilDialogActual =
                null
        }

        // =========================================================
        // CANCELAR
        // =========================================================

        btnCancelarPerfil.setOnClickListener {

            dialogo.dismiss()
        }

        // =========================================================
        // GUARDAR PERFIL
        // =========================================================

        btnGuardarPerfil.setOnClickListener {

            val nuevoNombre =
                capitalizarTexto(
                    etNombrePerfil.text
                        .toString()
                )

            val nuevoCorreo =
                etCorreoPerfil.text
                    .toString()
                    .trim()

            etNombrePerfil.error =
                null

            etCorreoPerfil.error =
                null

            // =====================================================
            // VALIDAR NOMBRE
            // =====================================================

            if (
                nuevoNombre.isEmpty()
            ) {

                etNombrePerfil.error =
                    "Ingrese su nombre"

                etNombrePerfil.requestFocus()

                return@setOnClickListener
            }

            // =====================================================
            // VALIDAR CORREO
            // =====================================================

            if (
                nuevoCorreo.isEmpty()
            ) {

                etCorreoPerfil.error =
                    "Ingrese su correo electrónico"

                etCorreoPerfil.requestFocus()

                return@setOnClickListener
            }

            if (
                !Patterns.EMAIL_ADDRESS
                    .matcher(
                        nuevoCorreo
                    )
                    .matches()
            ) {

                etCorreoPerfil.error =
                    "Ingrese un correo electrónico válido"

                etCorreoPerfil.requestFocus()

                return@setOnClickListener
            }

            etNombrePerfil.setText(
                nuevoNombre
            )

            btnGuardarPerfil.isEnabled =
                false

            btnGuardarPerfil.text =
                "GUARDANDO..."

            // =====================================================
            // ACTUALIZAR NOMBRE
            // =====================================================

            val perfil =
                userProfileChangeRequest {

                    displayName =
                        nuevoNombre
                }

            usuario.updateProfile(
                perfil
            ).addOnCompleteListener { tareaNombre ->

                if (
                    !tareaNombre.isSuccessful
                ) {

                    btnGuardarPerfil.isEnabled =
                        true

                    btnGuardarPerfil.text =
                        "GUARDAR"

                    Toast.makeText(
                        this,
                        "No se pudo actualizar el nombre",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnCompleteListener
                }

                val correoActual =
                    usuario.email ?: ""

                // =================================================
                // CORREO SIN CAMBIOS
                // =================================================

                if (
                    nuevoCorreo.equals(
                        correoActual,
                        ignoreCase = true
                    )
                ) {

                    btnGuardarPerfil.isEnabled =
                        true

                    btnGuardarPerfil.text =
                        "GUARDAR"

                    Toast.makeText(
                        this,
                        "Perfil actualizado correctamente",
                        Toast.LENGTH_LONG
                    ).show()

                    dialogo.dismiss()

                } else {

                    // =================================================
                    // CAMBIO DE CORREO
                    // =================================================

                    usuario.verifyBeforeUpdateEmail(
                        nuevoCorreo
                    ).addOnCompleteListener { tareaCorreo ->

                        btnGuardarPerfil.isEnabled =
                            true

                        btnGuardarPerfil.text =
                            "GUARDAR"

                        if (
                            tareaCorreo.isSuccessful
                        ) {

                            /*
                             * Firebase NO cambia el correo inmediatamente.
                             * Primero envía un enlace al nuevo correo.
                             * El cambio se completa cuando el usuario abre
                             * y confirma ese enlace.
                             */
                            AlertDialog.Builder(this)
                                .setTitle(
                                    "Confirma el nuevo correo"
                                )
                                .setMessage(
                                    "Se envió un enlace de verificación a:\n\n" +
                                            nuevoCorreo +
                                            "\n\nEl correo de tu cuenta todavía NO ha cambiado. " +
                                            "Primero debes abrir el enlace enviado por Firebase.\n\n" +
                                            "Después de verificarlo, vuelve a GreenHouse e inicia sesión " +
                                            "con el nuevo correo y la misma contraseña."
                                )
                                .setCancelable(false)
                                .setNegativeButton(
                                    "SEGUIR EN LA APP"
                                ) { mensaje, _ ->

                                    mensaje.dismiss()
                                }
                                .setPositiveButton(
                                    "CERRAR SESIÓN"
                                ) { _, _ ->

                                    dialogo.dismiss()

                                    cerrarSesion()
                                }
                                .show()

                        } else {

                            val excepcion =
                                tareaCorreo.exception

                            when (excepcion) {

                                is FirebaseAuthRecentLoginRequiredException -> {

                                    AlertDialog.Builder(this)
                                        .setTitle(
                                            "Vuelve a iniciar sesión"
                                        )
                                        .setMessage(
                                            "Por seguridad, Firebase requiere una autenticación reciente " +
                                                    "para cambiar el correo.\n\n" +
                                                    "Cierra sesión, vuelve a ingresar con tu correo actual " +
                                                    "y repite el cambio."
                                        )
                                        .setNegativeButton(
                                            "CANCELAR",
                                            null
                                        )
                                        .setPositiveButton(
                                            "CERRAR SESIÓN"
                                        ) { _, _ ->

                                            dialogo.dismiss()

                                            cerrarSesion()
                                        }
                                        .show()
                                }

                                is FirebaseAuthUserCollisionException -> {

                                    etCorreoPerfil.error =
                                        "Este correo ya pertenece a otra cuenta"

                                    etCorreoPerfil.requestFocus()
                                }

                                else -> {

                                    Toast.makeText(
                                        this,
                                        "No se pudo enviar la verificación del nuevo correo. Inténtalo nuevamente.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        dialogo.show()
    }

    // =============================================================
    // COMPROBACIÓN DEL SISTEMA
    // =============================================================

    private fun comprobarSistema() {

        val usuario =
            auth.currentUser

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_comprobacion_sistema,
                null
            )

        val tvSesion =
            vista.findViewById<TextView>(
                R.id.tvComprobacionSesion
            )

        val tvCuenta =
            vista.findViewById<TextView>(
                R.id.tvComprobacionCuenta
            )

        val tvConexion =
            vista.findViewById<TextView>(
                R.id.tvComprobacionConexion
            )

        val tvBomba =
            vista.findViewById<TextView>(
                R.id.tvComprobacionBomba
            )

        val tvVentilacion =
            vista.findViewById<TextView>(
                R.id.tvComprobacionVentilacion
            )

        val btnAceptar =
            vista.findViewById<Button>(
                R.id.btnAceptarComprobacion
            )

        // =========================================================
        // SESIÓN
        // =========================================================

        if (
            usuario != null
        ) {

            tvSesion.text =
                "✓ Correcta"

            tvSesion.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvSesion.text =
                "● No disponible"

            tvSesion.setTextColor(
                Color.parseColor(
                    "#C62828"
                )
            )
        }

        // =========================================================
        // CUENTA
        // =========================================================

        if (
            usuario?.isEmailVerified == true
        ) {

            tvCuenta.text =
                "✓ Verificada"

            tvCuenta.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvCuenta.text =
                "● No verificada"

            tvCuenta.setTextColor(
                Color.parseColor(
                    "#C62828"
                )
            )
        }

        // =========================================================
        // CONEXIÓN
        // =========================================================

        val conectado =
            tvEstadoConexion.text
                .toString()
                .contains(
                    "Conectado",
                    ignoreCase = true
                ) &&
                    !tvEstadoConexion.text
                        .toString()
                        .contains(
                            "Desconectado",
                            ignoreCase = true
                        )

        if (conectado) {

            tvConexion.text =
                "✓ Conectado"

            tvConexion.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvConexion.text =
                "● Desconectado"

            tvConexion.setTextColor(
                Color.parseColor(
                    "#C62828"
                )
            )
        }

        // =========================================================
        // BOMBA
        // =========================================================

        if (
            switchBomba.isChecked
        ) {

            tvBomba.text =
                "✓ Encendida"

            tvBomba.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvBomba.text =
                "Apagada"

            tvBomba.setTextColor(
                Color.parseColor(
                    "#616161"
                )
            )
        }

        // =========================================================
        // VENTILACIÓN
        // =========================================================

        if (
            switchVentilacion.isChecked
        ) {

            tvVentilacion.text =
                "✓ Encendida"

            tvVentilacion.setTextColor(
                Color.parseColor(
                    "#2E7D32"
                )
            )

        } else {

            tvVentilacion.text =
                "Apagada"

            tvVentilacion.setTextColor(
                Color.parseColor(
                    "#616161"
                )
            )
        }

        // =========================================================
        // DIÁLOGO
        // =========================================================

        val dialogo =
            AlertDialog.Builder(this)
                .setView(vista)
                .create()

        dialogo.setOnShowListener {

            dialogo.window
                ?.setBackgroundDrawableResource(
                    android.R.color.transparent
                )
        }

        btnAceptar.setOnClickListener {

            dialogo.dismiss()
        }

        dialogo.show()
    }

    // =============================================================
    // REINICIO DEL SISTEMA
    // =============================================================

    private fun mostrarConfirmacionReinicio() {

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_reinicio_sistema,
                null
            )

        val btnCancelar =
            vista.findViewById<Button>(
                R.id.btnCancelarReinicio
            )

        val btnReiniciar =
            vista.findViewById<Button>(
                R.id.btnConfirmarReinicio
            )

        val dialogo =
            AlertDialog.Builder(this)
                .setView(vista)
                .create()

        dialogo.setOnShowListener {

            dialogo.window
                ?.setBackgroundDrawableResource(
                    android.R.color.transparent
                )
        }

        btnCancelar.setOnClickListener {

            dialogo.dismiss()
        }

        btnReiniciar.setOnClickListener {

            dialogo.dismiss()

            reiniciarSistema()
        }

        dialogo.show()
    }

    // =============================================================
    // REALIZAR REINICIO
    // =============================================================

    private fun reiniciarSistema() {

        val valoresIniciales =
            mapOf<String, Any>(
                "bomba" to false,
                "ventilacion" to false
            )

        actuadoresRef
            .updateChildren(
                valoresIniciales
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Sistema reiniciado correctamente",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "No se pudo reiniciar el sistema",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =============================================================
    // ACERCA DE GREENHOUSE
    // =============================================================

    private fun mostrarAcercaDe() {

        val vista =
            layoutInflater.inflate(
                R.layout.dialog_acerca_greenhouse,
                null
            )

        val btnAceptar =
            vista.findViewById<Button>(
                R.id.btnAceptarAcerca
            )

        val dialogo =
            AlertDialog.Builder(this)
                .setView(vista)
                .create()

        dialogo.setOnShowListener {

            dialogo.window
                ?.setBackgroundDrawableResource(
                    android.R.color.transparent
                )
        }

        btnAceptar.setOnClickListener {

            dialogo.dismiss()
        }

        dialogo.show()
    }

    // =============================================================
    // CERRAR SESIÓN
    // =============================================================

    private fun mostrarConfirmacionCerrarSesion() {

        AlertDialog.Builder(this)
            .setTitle(
                "Cerrar sesión"
            )
            .setMessage(
                "¿Seguro que deseas cerrar tu sesión?"
            )
            .setNegativeButton(
                "CANCELAR"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .setPositiveButton(
                "CERRAR SESIÓN"
            ) { _, _ ->

                cerrarSesion()
            }
            .show()
    }

    private fun cerrarSesion() {

        auth.signOut()

        volverAlLogin()
    }

    // =============================================================
    // VOLVER AL LOGIN
    // =============================================================

    private fun volverAlLogin() {

        val intent =
            Intent(
                this,
                MainActivity::class.java
            )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(
            intent
        )

        finish()
    }

    // =============================================================
    // CORREGIR NOMBRE
    // =============================================================

    private fun capitalizarTexto(
        texto: String
    ): String {

        val locale =
            Locale(
                "es",
                "CL"
            )

        return texto
            .trim()
            .lowercase(
                locale
            )
            .split(
                Regex("\\s+")
            )
            .filter {
                it.isNotEmpty()
            }
            .joinToString(
                " "
            ) { palabra ->

                palabra.replaceFirstChar { caracter ->

                    if (
                        caracter.isLowerCase()
                    ) {

                        caracter.titlecase(
                            locale
                        )

                    } else {

                        caracter.toString()
                    }
                }
            }
    }

    // =============================================================
    // ELIMINAR LISTENERS AL CERRAR ACTIVITY
    // =============================================================

    override fun onDestroy() {
        super.onDestroy()

        listenerSensores?.let {

            sensoresRef.removeEventListener(
                it
            )
        }

        listenerActuadores?.let {

            actuadoresRef.removeEventListener(
                it
            )
        }

        listenerConexion?.let {

            conexionRef.removeEventListener(
                it
            )
        }
    }
}