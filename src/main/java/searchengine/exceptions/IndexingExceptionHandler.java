package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class IndexingExceptionHandler extends ResponseEntityExceptionHandler {
   @ExceptionHandler(StartIndexingException.class)
    protected ResponseEntity<ExceptionMessage> handleStartException() {
        return new ResponseEntity<>(new ExceptionMessage(false, "Индексация уже запущена"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(StopIndexingException.class)
    protected ResponseEntity <ExceptionMessage> handleStopIndexingException() {
       return new ResponseEntity<>(new ExceptionMessage(false, "Индексация не запущена"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OnePageIndexingException.class)
    protected ResponseEntity<ExceptionMessage> handlePageIndexingException() {
        return new ResponseEntity<>(new ExceptionMessage(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler(WrongPathException.class)
    protected ResponseEntity<ExceptionMessage> handleWrongPathException() {
       return new ResponseEntity<>(new ExceptionMessage(false, "Ошибка в написании url"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PageNotFoundException.class)
    protected ResponseEntity<ExceptionMessage> handleNotFoundException() {
       return new ResponseEntity<>(new ExceptionMessage(false, "Указанная страница не найдена"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OffTheListException.class)
    protected ResponseEntity<ExceptionMessage> handleOffTheListException() {
       return new ResponseEntity<>(new ExceptionMessage(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле"), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(EmptyQueryException.class)
    protected ResponseEntity<ExceptionMessage> handleEmptyQueryException() {
       return new ResponseEntity<>(new ExceptionMessage(false, "Задан пустой поисковый запрос"), HttpStatus.BAD_REQUEST);
    }

    }



