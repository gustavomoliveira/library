import styles from './HomePage.module.css';
import { BookList, FineList, Header, LoanList, UserList } from "../../components/index.js";

export default function HomePage() {
    return (
        <div className={styles.page}>
            <Header />
            <BookList />
            <UserList />
            <LoanList />
            <FineList />
        </div>
    )
}