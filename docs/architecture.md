# Arquitectura del Proyecto - TemUES

Este documento describe la arquitectura técnica, la organización de paquetes y las guías de desarrollo para la aplicación **TemUES**. Se ha seleccionado una arquitectura moderna que garantiza escalabilidad, mantenibilidad y facilidad de prueba.

## 🏛️ Patrón de Arquitectura: MVVM (Model-ViewModel-View)

La aplicación sigue el patrón **MVVM**, recomendado por Google, para separar la lógica de negocio de la interfaz de usuario:

1.  **Model (Modelo):** Representa los datos y la lógica de negocio. Incluye las entidades de Room, los modelos de respuesta de Firebase y las clases de datos de Kotlin.
2.  **View (Vista):** Activities y Fragments. Su única responsabilidad es mostrar datos y reaccionar a las interacciones del usuario. No contiene lógica de decisión.
3.  **ViewModel:** Actúa como puente. Solicita datos al repositorio y los expone a la vista mediante **LiveData** o **StateFlow**. Sobrevive a cambios de configuración (como rotar la pantalla).

## 📂 Organización de Paquetes

El código fuente se organiza por **capas y funcionalidades** dentro de `app/src/main/java/com/market/temues/`:

### 1. `data/` (Capa de Datos)
-   **`local/`**: Configuración de **Room Database**, DAOs y entidades para persistencia local.
-   **`remote/`**: Interfaces de **Retrofit** para APIs externas y servicios de **Firebase** (Auth, Firestore, Storage).
-   **`repository/`**: Clase mediadora que decide si obtener datos de la red o de la base de datos local (Single Source of Truth).

### 2. `model/` (Entidades)
-   Clases `data class` de Kotlin que representan los objetos del dominio: `Product`, `User`, `Message`, `Category`.

### 3. `ui/` (Interfaz de Usuario)
Organizado por carpetas según la funcionalidad (Features):
-   **`auth/`**: Login y Registro.
-   **`home/`**: Pantalla principal y lista de productos.
-   **`search/`**: Lógica de búsqueda y filtros.
-   **`chat/`**: Mensajería y lista de conversaciones.
-   **`profile/`**: Gestión de perfil de comprador/vendedor.
-   **`common/`**: Adapters de RecyclerView y componentes reutilizables.

### 4. `utils/` (Utilidades)
-   Extensiones de Kotlin, formateadores de moneda/fecha, y gestores de permisos (Cámara, Teléfono).

### 5. `ml/` (Inteligencia Artificial)
-   Implementación de los modelos de **TensorFlow Lite** para las recomendaciones personalizadas.

## 🛠️ Guía de Desarrollo: ¿Cómo y dónde programar?

Para mantener la consistencia, sigue este flujo al desarrollar una nueva funcionalidad:

1.  **Modelo:** Define la clase de datos en `model/`.
2.  **Fuente de Datos:** Crea la interfaz en `data/remote/` o el DAO en `data/local/`.
3.  **Repositorio:** Añade las funciones necesarias en el repositorio para manejar los datos.
4.  **ViewModel:** Crea el ViewModel correspondiente en la carpeta de la funcionalidad (ej. `ui/home/HomeViewModel`). Implementa la lógica aquí.
5.  **Layout:** Diseña la interfaz en `res/layout/`.
6.  **Vista:** Implementa el Fragment/Activity usando **ViewBinding** para conectar con el ViewModel.

## 🚦 Reglas de Oro
-   **No lógicas en la Vista:** Si hay un `if` o un cálculo complejo, debe ir en el ViewModel.
-   **ViewBinding siempre:** Evita el uso de `findViewById`.
-   **Inyección de Dependencias (Hilt):** El proyecto usa **Hilt**. Anota Activities/Fragments con `@AndroidEntryPoint`, ViewModels con `@HiltViewModel`, y crea módulos con `@Module` para proveer dependencias.
-   **Seguridad:** Toda comunicación con Firebase debe pasar por las reglas de seguridad definidas en la consola de Firebase.

---
Este diseño permite que **TemUES** crezca sin convertirse en código difícil de manejar, permitiendo que cada componente tenga una responsabilidad única y clara.
