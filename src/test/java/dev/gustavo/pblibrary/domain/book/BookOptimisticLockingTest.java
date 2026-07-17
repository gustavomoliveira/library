package dev.gustavo.pblibrary.domain.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class BookOptimisticLockingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void saveAndFlush_deveLancarExcecao_quandoDuasTransacoesAlteramMesmoLivroConcorrentemente() {
        Book book = bookRepository.save(new Book("Effective Java", "Joshua Bloch", "978-0134685991", 5));
        entityManager.flush();
        Long id = book.getId();

        entityManager.clear();
        Book copy1 = bookRepository.findById(id).orElseThrow();

        entityManager.clear();
        Book copy2 = bookRepository.findById(id).orElseThrow();

        entityManager.clear();

        copy1.setAvailableCopies(4);
        bookRepository.saveAndFlush(copy1);
        entityManager.clear();

        copy2.setAvailableCopies(3);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> bookRepository.saveAndFlush(copy2));
    }
}