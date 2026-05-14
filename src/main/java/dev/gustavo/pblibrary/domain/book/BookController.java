package dev.gustavo.pblibrary.domain.book;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<BookResponseDTO> createBook(@RequestBody BookDTO dto) {
         BookResponseDTO response = service.createBook(dto);
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponseDTO> updateBook(@PathVariable Long id, @RequestBody BookDTO dto) {
        BookResponseDTO response = service.updateBook(id, dto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        service.deleteBook(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookResponseDTO> getBookByIsbn(@PathVariable String isbn) {
        BookResponseDTO response = service.findBookByIsbn(isbn);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDTO>> getBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author) {
        List<BookResponseDTO> response = service.findBooks(title, author);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
