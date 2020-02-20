**Тестирование архитектур серверов**

1. Клиент устанавливает постоянное соединение. Сервер создает отдельный поток на общение (прием запроса, выполнение запроса и отправку ответа) с конкретным клиентом.
2. Клиент устанавливает постоянное соединение. Сервер создает по отдельному  потоку на каждого клиента для приема от него данных + по одному SingleThreadExecutor для отсылки данных клиенту. Полученные от клиента запросы попадают в общий пул потоков фиксированного размера. После обработки ответ клиенту отправляется через соответствующий SingleThreadExecutor.
3. Клиент устанавливает постоянное соединение. Сервер производит неблокирующую обработку. Каждый запрос обрабатывается пуле потоков фиксированного размера. Сервер работает с сокетами в однопоточном режиме (один поток и селектор на прием всех сообщений и один поток и селектор на отправку всех сообщений).

**Метрики**

1. Время обработки запроса (сортировки в данном случае) на сервере, ms (считается с момента начала обработки до момента окончания обработки)
1. Время обработки клиента на сервере, ms (считается с момента получения запроса от клиента до момента отсылки результата клиенту)
1. Среднее время одного запроса на клиенте, ms. Считаем время от старта клиента до конца его работы, делим на X. Усредняем по всем клиентам.

**Запуск приложения:**

1. Прописать в классе Constants переменные (HOST_1, PORT_1), (HOST_2, PORT_2), (HOST_3, PORT_3). Каждая пара отвечает за адрес сервера с соответствующей по номеру архитектрой. По умолчанию HOST_i = localhost.
2. Проверить, что необходимые для тестирования серверы запущены.
3. Запустить класс Main (откроется окно GUI).
4. Выбрать необходимые для запуска параметры (их набор соответсвует с набором в описании к заданию).
5. После нажатия на кнопку появится новое окно с 3 графиками.
6. Автоматически создадутся три файла с полученными значениями (в первой строке прописаны значения параметров запуска).

**Недостатки**

*их можно было бы изжать, но на это нужно еще время*
* довольно некрасивый GUI
* если закрыть окно с графиками - закроется все приложение (но если не закрывать, то можно тестировать дальше)
* полученные графики выглядят немного сомнительно (возможно причина во входных параметрах)

**Графики**

Все 9 графиков собраны в файл graphics.png в корне репозитория. Значения, по которым они построены в папке results. Файлы именуются так - res_номер архитектуры сервера_по какому параметру_номер метрики.
