# Diccionario de Datos — ITSX CHAT

Este diccionario describe las entidades, campos y relaciones del modelo de base de datos diseñado para ITSX CHAT.

---

## Estructura General

| Campo | Tipo de Dato | Longitud / Formato | Clave | Nulos | Descripción |
|--------|---------------|--------------------|--------|--------|--------------|
| **id_dispositivo** | INTEGER | — | PK | No | Identificador único autoincremental del dispositivo. |
| **uuid** | TEXT | 36 |  | No | Identificador universal único del dispositivo (UUID). |
| **direccion_dispositivo** | TEXT | 255 |  | No | Dirección IP o identificador de red del dispositivo. |
| **nombre_dispositivo** | TEXT | 100 |  | No | Nombre amigable asignado al dispositivo. |
| **esLocal** | BOOLEAN | — |  | No | Indica si el dispositivo es local (1) o remoto (0). |
| **ultima_conexion** | DATETIME | — |  | Sí | Fecha y hora de la última conexión del dispositivo. |
| **id_chat** | INTEGER | — | PK | No | Identificador único del chat. |
| **fecha_creacion** | DATETIME | — |  | No | Fecha y hora en que se creó el chat. |
| **id_chatParticipante** | INTEGER | — | PK | No | Identificador único de la participación (relación Chat–Dispositivo). |
| **rol** | TEXT | 20 |  | Sí | Rol del participante en el chat (por ejemplo: "admin", "miembro"). |
| **ultima_lectura** | DATETIME | — |  | Sí | Fecha y hora del último mensaje leído por el participante. |
| **idMensaje** | INTEGER | — | PK | No | Identificador único del mensaje. |
| **uuidRemitente** | TEXT | 36 |  | No | UUID del dispositivo que envió el mensaje. |
| **contenido** | TEXT | — |  | Sí | Texto del mensaje (puede estar vacío si contiene solo archivos). |
| **horaMensaje** | DATETIME | — |  | No | Fecha y hora en que se envió el mensaje. |
| **estado** | TEXT | 20 |  | No | Estado del mensaje: `enviando`, `enviado`, `recibido`, `leído`. |
| **idArchivo** | INTEGER | — | PK | No | Identificador único del archivo. |
| **nombreArchivo** | TEXT | 150 |  | No | Nombre del archivo enviado. |
| **tamañoArchivo** | INTEGER | — |  | No | Tamaño del archivo en bytes. |
| **rutaArchivo** | TEXT | 255 |  | No | Ruta o URI donde se almacena el archivo. |
| **estadoTransferencia** | TEXT | 20 |  | No | Estado de la transferencia: `pendiente`, `enviando`, `completo`, `fallido`. |
| **FK_id_dispositivo** | INTEGER | — | FK | Sí | Referencia a `Dispositivo(id_dispositivo)` en `ChatParticipante`. |
| **FK_id_chat** | INTEGER | — | FK | Sí | Referencia a `Chat(id_chat)` en `ChatParticipante` y `Mensaje`. |
| **FK_idMensaje** | INTEGER | — | FK | Sí | Referencia a `Mensaje(idMensaje)` en `Archivo`. |

---

## Relaciones Entre Tablas

| Relación | Tipo | Descripción |
|-----------|------|-------------|
| `ChatParticipante.id_dispositivo → Dispositivo.id_dispositivo` | 1:N | Un dispositivo puede participar en varios chats. |
| `ChatParticipante.id_chat → Chat.id_chat` | 1:N | Un chat puede tener varios participantes. |
| `Mensaje.idChat → Chat.id_chat` | 1:N | Un chat contiene muchos mensajes. |
| `Archivo.idMensaje → Mensaje.idMensaje` | 1:N | Un mensaje puede contener varios archivos. |

---

## Notas Generales

- **Normalización:** Cumple con la **Tercera Forma Normal (3FN)**.  
- **Integridad:** Asegurada mediante claves primarias y foráneas.  
- **Escalabilidad:** Soporta chats individuales y grupales, adjuntos, y control de estados.  
- **Compatibilidad:** Ideal para implementarse en **SQLite / Room (Android)**.  

---

## Recomendaciones de Implementación

```sql
-- Restricción para evitar duplicados de dispositivos en un chat
UNIQUE (id_chat, id_dispositivo);

-- Validación de estados permitidos
CHECK (estado IN ('enviando', 'enviado', 'recibido', 'leido'));
CHECK (estadoTransferencia IN ('pendiente', 'enviando', 'completo', 'fallido'));
```
## Diagrama relacional de la base de datos
<img width="750" height="531" alt="bd_proyecto" src="https://github.com/user-attachments/assets/0d34f68f-a24e-4ad4-90e7-f87662528473" />
