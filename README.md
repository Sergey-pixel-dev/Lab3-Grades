# Лабораторная работа №3: Оптимизированная версия

**Вариант 8: Учет студентов - вычисление среднего балла (ОПТИМИЗИРОВАННАЯ ВЕРСИЯ)**

Это оптимизированная версия микросервисов из Лабораторной работы №2.

## Отличия от lab2-grades

- **lab2-grades** - исходная версия с намеренно неоптимальным кодом (для профилирования)
- **lab3-grades** - оптимизированная версия после профилирования и устранения узких мест

---

## Отчет по профилированию и оптимизации

### Цель работы

Научиться использовать инструменты профилирования Java приложений (Java Flight Recorder, Java Mission Control, VisualVM, Async Profiler) для поиска и устранения узких мест производительности в микросервисной архитектуре на Spring Boot.

### Результаты профилирования

![Method profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/method_prof_lab2.png)

![Memory profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/mem_lab2.png)

![SQL profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/sql_lab2.png)


### Примененные оптимизации

Таблица оптимизаций, реализованных в lab3-grades:

| Было (lab2) | Стало (lab3) | Обоснование |
|-------------|--------------|-------------|
| `gradeRepository.findAll()` | `gradeRepository.findStudentStatsByCourseName(courseName)` | SQL WHERE фильтрация эффективнее in-memory фильтрации |
| N+1 запросы (1000+ SELECT) | Один JOIN запрос с GROUP BY | Сокращение сетевых вызовов к БД в 200 раз |
| O(n²) циклы для группировки | SQL GROUP BY | СУБД оптимизирована для агрегации данных |
| In-memory вычисление AVG | SQL AVG() функция | Использование встроенных возможностей БД |
| Множественные сортировки | Одна сортировка ORDER BY в SQL | Минимизация операций сортировки |
| Stream API для больших данных | SQL LIMIT для топ-10 | Обработка данных на уровне БД |

### Фрагменты кода до и после оптимизации

**До оптимизации (lab2-grades/service-b/src/main/java/com/example/server/service/GradeService.java):**

```java
// ПРОБЛЕМА #1: Загрузка всех 100,000+ записей в память
List<Grade> allGrades = gradeRepository.findAll();

// ПРОБЛЕМА #2: Две ненужные сортировки всего массива
List<Grade> sorted1 = allGrades.stream()
    .sorted(Comparator.comparing(Grade::getGrade).reversed())
    .toList();
List<Grade> sorted2 = sorted1.stream()
    .sorted(Comparator.comparing(Grade::getGrade))
    .toList();

// ПРОБЛЕМА #3: In-memory фильтрация вместо SQL WHERE
List<Grade> filteredGrades = allGrades.stream()
    .filter(grade -> grade.getCourse().getName().equals(courseName))
    .toList();

// ПРОБЛЕМА #4: O(n²) извлечение уникальных ID через вложенные циклы
Set<Long> uniqueStudentIds = new HashSet<>();
for (Grade grade : filteredGrades) {
    Long studentId = grade.getStudent().getId();
    boolean isDuplicate = false;
    for (Long existingId : uniqueStudentIds) {
        if (existingId.equals(studentId)) {
            isDuplicate = true;
            break;
        }
    }
    if (!isDuplicate) {
        uniqueStudentIds.add(studentId);
    }
}

// ПРОБЛЕМА #5: N+1 запросы - каждый студент загружается отдельным SELECT
for (Long studentId : uniqueStudentIds) {
    Student student = studentRepository.findById(studentId).orElse(null);
    // обработка...
}
```

**После оптимизации (lab3-grades/service-b/src/main/java/com/example/server/service/GradeService.java):**

```java
// РЕШЕНИЕ: Один SQL запрос с JOIN, GROUP BY и агрегацией
List<StudentGradeStats> studentStats =
    gradeRepository.findStudentStatsByCourseName(courseName);

// Расчет общих метрик одним запросом
Double averageGrade = gradeRepository.findAverageGradeByCourseName(courseName);
Integer totalGrades = gradeRepository.countByCourseName(courseName);
Integer totalStudents = gradeRepository.countDistinctStudentsByCourseName(courseName);

// Топ-10 студентов уже получены отсортированными из БД
List<AverageGradeResponse.StudentGradeInfo> topStudents = studentStats.stream()
    .limit(10)
    .map(stat -> new AverageGradeResponse.StudentGradeInfo(
        stat.getStudentId(),
        stat.getStudentName(),
        stat.getAverageGrade(),
        stat.getGradeCount().intValue()
    ))
    .toList();
```

**GradeRepository.java - оптимизированные запросы:**

```java
@Query("""
    SELECT new com.example.server.dto.StudentGradeStats(
        s.id, s.name, AVG(g.grade), COUNT(g.id)
    )
    FROM Grade g
    JOIN g.student s
    JOIN g.course c
    WHERE c.name = :courseName
    GROUP BY s.id, s.name
    ORDER BY AVG(g.grade) DESC
    """)
List<StudentGradeStats> findStudentStatsByCourseName(String courseName);

@Query("SELECT AVG(g.grade) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
Double findAverageGradeByCourseName(String courseName);

@Query("SELECT COUNT(g) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
Integer countByCourseName(String courseName);

@Query("SELECT COUNT(DISTINCT g.student.id) FROM Grade g JOIN g.course c WHERE c.name = :courseName")
Integer countDistinctStudentsByCourseName(String courseName);
```

![Method profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/method_prof_lab3.png)

![Memory profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/mem_lab3.png)

![SQL profile](https://github.com/Sergey-pixel-dev/Lab3-Grades/raw/main/sql_lab3.png)

### Выводы

В ходе лабораторной работы были изучены инструменты профилирования Java приложений: Java Flight Recorder (JFR), Java Mission Control (JMC).

С помощью этих инструментов в микросервисе вычисления среднего балла студентов были обнаружены критические проблемы производительности:
1. **Загрузка всех 100,000+ оценок в память** вместо фильтрации на уровне БД (18+ секунд)
2. **N+1 проблема** при загрузке данных студентов (1000+ отдельных SQL запросов)
3. **Вложенные циклы O(n²)** для группировки данных в памяти приложения
4. **Множественные ненужные сортировки** больших массивов данных

**Основной вывод**: критически важно использовать возможности СУБД (WHERE, JOIN, GROUP BY, агрегатные функции) вместо загрузки всех данных в память приложения и обработки их программными средствами. Это особенно важно при работе с большими объемами данных (100,000+ записей). Делегирование работы с данными на уровень БД снижает нагрузку на приложение, уменьшает потребление памяти и сокращает количество сетевых вызовов.

---

## Требования

- **Docker** и **Docker Compose**
- **Make** (опционально, для Linux/macOS)
- Свободные порты: 8080, 8081, 5433

## Запуск (Linux / macOS / Windows)

### Linux / macOS с Make

```bash
# Запуск всех сервисов
make up

# Проверка статуса
make health

# Тест
make test

# Остановка
make down
```

### Windows (PowerShell / CMD)

```powershell
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down
```

---

## Проверка результатов

### До оптимизации (lab2-grades):
```json
{
  "courseName": "Mathematics",
  "averageGrade": 49.8,
  "totalStudents": 995,
  "totalGrades": 9876,
  "topStudents": [...],
  "processingTimeMs": 18234
}
```

### После оптимизации (lab3-grades):
```json
{
  "courseName": "Mathematics",
  "averageGrade": 49.8,
  "totalStudents": 995,
  "totalGrades": 9876,
  "topStudents": [...],
  "processingTimeMs": 156
}
```

---

## API Endpoints

### Service A (Client) - :8080

| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/api/client/grades/average/{courseName}` | Получить средний балл по курсу |
| GET | `/api/client/health` | Health check Service A |
| GET | `/api/client/health/service-b` | Проверить доступность Service B |

### Service B (Server) - :8081

| Method | Endpoint | Описание |
|--------|----------|----------|
| GET | `/api/grades/average/{courseName}` | Рассчитать средний балл (ОПТИМИЗИРОВАННЫЙ) |
| GET | `/api/grades/health` | Health check Service B |

---

## Доступные курсы

- Mathematics
- Physics
- Computer Science
- Chemistry
- Biology
- English
- History
- Philosophy
- Economics
- Statistics
