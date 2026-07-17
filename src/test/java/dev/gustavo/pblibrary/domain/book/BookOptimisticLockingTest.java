package dev.gustavo.pblibrary.domain.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simula duas "sessões" lendo o mesmo Book e tentando escrever nele.
 * O entityManager.clear() é o que força o Hibernate a tratar copy1 e copy2
 * como instâncias completamente separadas, cada uma com sua própria versão
 * lida no momento do SELECT — reproduzindo o que aconteceria com duas
 * requisições HTTP concorrentes.
 */
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

        copy1.setAvailableCopies(4);
        bookRepository.saveAndFlush(copy1);

        copy2.setAvailableCopies(3);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> bookRepository.saveAndFlush(copy2));
    }
}
