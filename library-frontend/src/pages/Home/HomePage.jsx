import styles from './HomePage.module.css';
import { BookList, Header, LoanList, UserList } from "../../components/index.js";

export default function HomePage() {
    return (
        <div className={styles.page}>
            <Header />
            <BookList />
            <UserList />
            <LoanList />
        </div>
    )
}