import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="mx-auto mt-24 max-w-md text-center">
      <p className="font-mono text-3xl text-fg-muted">404</p>
      <p className="mt-2">This page does not exist.</p>
      <Link to="/" className="mt-4 inline-block text-accent hover:underline">
        Go to the home page
      </Link>
    </div>
  );
}
