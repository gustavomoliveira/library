import styles from './BookList.module.css';
import { getAllBooks } from "../../services/index.js";
import { useEffect, useState } from "react";

export default function BookList() {
    const [books, setBooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await getAllBooks();
                setBooks(response);
            } catch (e) {
                setError(e);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    return (
        <>
            {error ? (
                <p className={styles.errorMessage}>Erro ao carregar dados de livros</p>
            ) : loading ? (
                <p className={styles.loadingMessage}>Carregando...</p>
            ) : (
                <section className={styles.section}>
                    <div className={styles.sectionHeader}>
                        <h2 className={styles.sectionTitle}>Acervo</h2>
                        <span className={styles.count}>{books.length} títulos</span>
                    </div>
                    {books.length === 0 ? (
                        <p className={styles.emptyMessage}>Nenhum livro encontrado.</p>
                    ) : (
                        <ul className={styles.list}>
                            {books.map((book) => (
                                <li key={book.id} className={styles.card}>
                                    <p className={styles.bookTitle}>{book.title}</p>
                                    <p className={styles.bookAuthor}>{book.author}</p>
                                    <div className={styles.copies}>
                                        <span className={styles.copyBadge}>Total: <strong>{book.totalCopies}</strong></span>
                                        <span className={styles.copyBadge}>Disponíveis: <strong>{book.availableCopies}</strong></span>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            )}
        </>
    );
}