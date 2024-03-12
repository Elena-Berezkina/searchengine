Задача проекта - реализовать поисковый движок (реализация back-end составляющей), который будет осуществлять поиск по заданному списку сайтов. Приложение построено на платформе Spring Boot и предполагает использование MySql. 

Список сайтов задан в конфигурационном файле application.yaml, находящемся в корне проекта.

Функционал приложения:
- выдача статистики(команда "/api/statistics") - предоставление данных по проиндексированным или индексируемым сайтам, страницам, а также полученным леммам (формам встречающихся на старницах слов) и индексам (связям лемм и страниц);
- индексация сайтов(команда "/api/startIndexing) - обход сайтов с занесением данных по сайтам и каждой странице каждого сайта в соответствующие таблицы БД search_engine в MySql(site, page, lemma, lemma_index);
- остановка индексации(команда "/api/stopIndexing");
- индексация, переиндексация или создание (если не существовала ранее) конкретной страницы("/api/indexPage);
- поиск по всем сайтам("/api/search", параметр - query);
- поиск по одному сайту("api/siteSearch", параметры: query, path).

Ошибки выдаются в следующих случаях:
- индексация сайтов, которые уже индексируются;
- остановка индексации, если индексация не запущена;
- поиск по сайту, не включенному в список, определенный файлом конфигурации;
- поиск по несуществующему сайту(страница не существует либо путь введен с ошибкой);
- пустой запрос или пустой адрес, если данные параметры предполагаются командой.

Использованные maven зависимости: org.springframework.boot, org.projectlombok, mysql, org.apache.lucene, org.apache.lucene.morphology, org.jsoup.# search-engine-repository